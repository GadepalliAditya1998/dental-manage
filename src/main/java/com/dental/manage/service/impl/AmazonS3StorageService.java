package com.dental.manage.service.impl;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.dto.response.StorageMetadataResponse;
import com.dental.manage.dto.response.StoredFileResponse;
import com.dental.manage.enums.StorageType;
import com.dental.manage.exception.FileNotFoundStorageException;
import com.dental.manage.exception.StorageException;
import com.dental.manage.service.StorageService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Concrete implementation of StorageService targeting Amazon S3 buckets.
 * Supports S3 operations, pre-signed URL generation, and custom/MinIO endpoints.
 */
@Slf4j
@Service("amazonS3StorageService")
public class AmazonS3StorageService implements StorageService {

    private final StorageProperties storageProperties;
    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private final boolean clientInjected;

    @Autowired
    public AmazonS3StorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.clientInjected = false;
    }

    public AmazonS3StorageService(StorageProperties storageProperties, S3Client s3Client, S3Presigner s3Presigner) {
        this.storageProperties = storageProperties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.clientInjected = true;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.S3;
    }

    @Override
    public StoredFileResponse uploadFile(MultipartFile file, String folder) {
        return uploadFile(file, folder, Collections.emptyMap());
    }

    @Override
    public StoredFileResponse uploadFile(MultipartFile file, String folder, Map<String, String> customMetadata) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Failed to store empty file to S3");
        }
        try {
            return uploadFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    folder,
                    customMetadata
            );
        } catch (IOException e) {
            throw new StorageException("Failed to read input stream from multipart file", e);
        }
    }

    @Override
    public StoredFileResponse uploadFile(InputStream inputStream, String fileName, String contentType, long size, String folder) {
        return uploadFile(inputStream, fileName, contentType, size, folder, Collections.emptyMap());
    }

    @Override
    public StoredFileResponse uploadFile(InputStream inputStream, String fileName, String contentType, long size, String folder, Map<String, String> customMetadata) {
        if (inputStream == null) {
            throw new StorageException("Failed to store file to S3: InputStream is null");
        }

        String safeFileName = sanitizeFileName(fileName);
        String fileKey = generateFileKey(folder, safeFileName);
        String bucket = getBucketName();
        String detectedContentType = resolveContentType(safeFileName, contentType);

        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .contentType(detectedContentType);

            if (size > 0) {
                requestBuilder.contentLength(size);
            }

            if (customMetadata != null && !customMetadata.isEmpty()) {
                requestBuilder.metadata(customMetadata);
            }

            RequestBody requestBody = (size > 0)
                    ? RequestBody.fromInputStream(inputStream, size)
                    : RequestBody.fromBytes(inputStream.readAllBytes());

            getS3Client().putObject(requestBuilder.build(), requestBody);

            String fileUrl = getFileUrl(fileKey);
            log.info("File successfully uploaded to Amazon S3: bucket={}, key={}", bucket, fileKey);

            return StoredFileResponse.builder()
                    .fileKey(fileKey)
                    .fileName(safeFileName)
                    .contentType(detectedContentType)
                    .size(size > 0 ? size : requestBody.contentLength())
                    .url(fileUrl)
                    .storageType(StorageType.S3)
                    .uploadedAt(LocalDateTime.now())
                    .metadata(customMetadata != null ? customMetadata : Collections.emptyMap())
                    .build();
        } catch (S3Exception e) {
            throw new StorageException("AWS S3 error while uploading file: " + fileKey + ". Reason: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new StorageException("Failed to upload file to S3: " + fileKey, e);
        }
    }

    @Override
    public StoredFileResponse uploadFile(byte[] content, String fileName, String contentType, String folder) {
        if (content == null || content.length == 0) {
            throw new StorageException("Failed to store empty byte array to S3");
        }
        return uploadFile(new ByteArrayInputStream(content), fileName, contentType, content.length, folder);
    }

    @Override
    public Resource downloadFile(String fileKey) {
        validateFileKey(fileKey);
        String bucket = getBucketName();

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            byte[] fileBytes;
            try (ResponseInputStream<GetObjectResponse> s3Stream = getS3Client().getObject(getObjectRequest)) {
                fileBytes = s3Stream.readAllBytes();
            }
            String fileName = extractFileName(fileKey);

            return new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundStorageException("File not found in S3 bucket " + bucket + ": " + fileKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new FileNotFoundStorageException("File not found in S3 bucket " + bucket + ": " + fileKey, e);
            }
            throw new StorageException("AWS S3 error while downloading file: " + fileKey + ". Reason: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            throw new StorageException("Failed to read S3 object stream for key: " + fileKey, e);
        }
    }

    @Override
    public StorageMetadataResponse getMetadata(String fileKey) {
        validateFileKey(fileKey);
        String bucket = getBucketName();

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            HeadObjectResponse head = getS3Client().headObject(headObjectRequest);
            LocalDateTime lastModified = head.lastModified() != null
                    ? LocalDateTime.ofInstant(head.lastModified(), ZoneId.systemDefault())
                    : LocalDateTime.now();

            return StorageMetadataResponse.builder()
                    .fileKey(fileKey)
                    .fileName(extractFileName(fileKey))
                    .contentType(head.contentType())
                    .size(head.contentLength())
                    .storageType(StorageType.S3)
                    .lastModified(lastModified)
                    .eTag(head.eTag())
                    .customMetadata(head.metadata() != null ? head.metadata() : Collections.emptyMap())
                    .build();
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundStorageException("File not found in S3: " + fileKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new FileNotFoundStorageException("File not found in S3: " + fileKey, e);
            }
            throw new StorageException("AWS S3 error reading metadata for key: " + fileKey, e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        validateFileKey(fileKey);
        String bucket = getBucketName();

        if (!exists(fileKey)) {
            throw new FileNotFoundStorageException("File does not exist in S3 for deletion: " + fileKey);
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            getS3Client().deleteObject(deleteRequest);
            log.info("File successfully deleted from Amazon S3: bucket={}, key={}", bucket, fileKey);
        } catch (S3Exception e) {
            throw new StorageException("AWS S3 error deleting key: " + fileKey + ". Reason: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String fileKey) {
        validateFileKey(fileKey);
        String bucket = getBucketName();
        StorageProperties.S3Properties s3Props = storageProperties.getS3();

        if (StringUtils.hasText(s3Props.getEndpoint())) {
            String endpoint = s3Props.getEndpoint().replaceAll("/+$", "");
            return endpoint + "/" + bucket + "/" + fileKey;
        }

        String region = StringUtils.hasText(s3Props.getRegion()) ? s3Props.getRegion() : "us-east-1";
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, fileKey);
    }

    @Override
    public String generatePresignedUrl(String fileKey, Duration expiration) {
        validateFileKey(fileKey);
        String bucket = getBucketName();
        Duration effectiveDuration = (expiration != null && !expiration.isNegative() && !expiration.isZero())
                ? expiration
                : Duration.ofMinutes(storageProperties.getS3().getPresignedUrlExpirationMinutes());

        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(effectiveDuration)
                    .getObjectRequest(b -> b.bucket(bucket).key(fileKey))
                    .build();

            PresignedGetObjectRequest presigned = getS3Presigner().presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned S3 URL for key: " + fileKey, e);
        }
    }

    @Override
    public List<StorageMetadataResponse> listFiles(String folderOrPrefix) {
        String bucket = getBucketName();
        String prefix = (folderOrPrefix != null) ? folderOrPrefix.trim().replaceAll("^/+", "") : "";

        try {
            ListObjectsV2Request.Builder reqBuilder = ListObjectsV2Request.builder().bucket(bucket);
            if (StringUtils.hasText(prefix)) {
                reqBuilder.prefix(prefix);
            }

            ListObjectsV2Response response = getS3Client().listObjectsV2(reqBuilder.build());
            List<StorageMetadataResponse> results = new ArrayList<>();

            for (S3Object s3Object : response.contents()) {
                LocalDateTime modified = s3Object.lastModified() != null
                        ? LocalDateTime.ofInstant(s3Object.lastModified(), ZoneId.systemDefault())
                        : LocalDateTime.now();

                results.add(StorageMetadataResponse.builder()
                        .fileKey(s3Object.key())
                        .fileName(extractFileName(s3Object.key()))
                        .contentType(resolveContentType(s3Object.key(), null))
                        .size(s3Object.size())
                        .storageType(StorageType.S3)
                        .lastModified(modified)
                        .eTag(s3Object.eTag())
                        .customMetadata(Collections.emptyMap())
                        .build());
            }

            return results;
        } catch (S3Exception e) {
            throw new StorageException("AWS S3 error listing objects with prefix: " + folderOrPrefix, e);
        }
    }

    @Override
    public boolean exists(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return false;
        }
        try {
            getS3Client().headObject(HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(fileKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new StorageException("Error checking S3 file existence for key: " + fileKey, e);
        }
    }

    public boolean isConfigured() {
        StorageProperties.S3Properties s3Props = storageProperties.getS3();
        return s3Props != null
                && StringUtils.hasText(s3Props.getBucket())
                && (StringUtils.hasText(s3Props.getAccessKey()) || StringUtils.hasText(s3Props.getEndpoint()));
    }

    private synchronized S3Client getS3Client() {
        if (this.s3Client == null) {
            this.s3Client = buildS3Client();
        }
        return this.s3Client;
    }

    private synchronized S3Presigner getS3Presigner() {
        if (this.s3Presigner == null) {
            this.s3Presigner = buildS3Presigner();
        }
        return this.s3Presigner;
    }

    private S3Client buildS3Client() {
        StorageProperties.S3Properties s3Props = storageProperties.getS3();
        try {
            S3ClientBuilder builder = S3Client.builder();
            String region = StringUtils.hasText(s3Props.getRegion()) ? s3Props.getRegion() : "us-east-1";
            builder.region(Region.of(region));

            if (StringUtils.hasText(s3Props.getEndpoint())) {
                builder.endpointOverride(URI.create(s3Props.getEndpoint()));
            }

            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(s3Props.isPathStyleAccess())
                    .build());

            if (StringUtils.hasText(s3Props.getAccessKey()) && StringUtils.hasText(s3Props.getSecretKey())) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())
                ));
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }

            log.info("Initialized Amazon S3 Client (bucket={}, region={})", s3Props.getBucket(), region);
            return builder.build();
        } catch (Exception e) {
            throw new StorageException("Failed to initialize AWS S3 client: " + e.getMessage(), e);
        }
    }

    private S3Presigner buildS3Presigner() {
        StorageProperties.S3Properties s3Props = storageProperties.getS3();
        try {
            S3Presigner.Builder builder = S3Presigner.builder();
            String region = StringUtils.hasText(s3Props.getRegion()) ? s3Props.getRegion() : "us-east-1";
            builder.region(Region.of(region));

            if (StringUtils.hasText(s3Props.getEndpoint())) {
                builder.endpointOverride(URI.create(s3Props.getEndpoint()));
            }

            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(s3Props.isPathStyleAccess())
                    .build());

            if (StringUtils.hasText(s3Props.getAccessKey()) && StringUtils.hasText(s3Props.getSecretKey())) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())
                ));
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }

            log.info("Initialized Amazon S3 Presigner (region={})", region);
            return builder.build();
        } catch (Exception e) {
            throw new StorageException("Failed to initialize AWS S3 presigner: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (!clientInjected) {
            if (s3Client != null) {
                try {
                    s3Client.close();
                } catch (Exception ignored) {}
            }
            if (s3Presigner != null) {
                try {
                    s3Presigner.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private String getBucketName() {
        String bucket = storageProperties.getS3().getBucket();
        if (!StringUtils.hasText(bucket)) {
            throw new StorageException("Amazon S3 bucket name is not configured. Please set 'storage.s3.bucket'.");
        }
        return bucket;
    }

    private void validateFileKey(String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            throw new StorageException("File key cannot be null or empty");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed-file";
        }
        String clean = StringUtils.cleanPath(fileName);
        int lastSlash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        return clean.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String generateFileKey(String folder, String safeFileName) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String nameWithId = uniqueId + "_" + safeFileName;
        if (folder != null && !folder.isBlank()) {
            String cleanFolder = folder.trim().replaceAll("^/+|/+$", "");
            return cleanFolder + "/" + nameWithId;
        }
        return nameWithId;
    }

    private String extractFileName(String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            return "unnamed-file";
        }
        int lastSlash = fileKey.lastIndexOf('/');
        return (lastSlash >= 0) ? fileKey.substring(lastSlash + 1) : fileKey;
    }

    private String resolveContentType(String fileName, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return MediaTypeFactory.getMediaType(fileName)
                .map(Object::toString)
                .orElse("application/octet-stream");
    }
}
