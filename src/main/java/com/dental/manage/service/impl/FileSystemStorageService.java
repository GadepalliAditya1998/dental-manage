package com.dental.manage.service.impl;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.dto.response.StorageMetadataResponse;
import com.dental.manage.dto.response.StoredFileResponse;
import com.dental.manage.enums.StorageType;
import com.dental.manage.exception.FileNotFoundStorageException;
import com.dental.manage.exception.StorageException;
import com.dental.manage.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Local FileSystem implementation of StorageService.
 * Stores and manages files on the local disk with path traversal protection.
 */
@Slf4j
@Service("fileSystemStorageService")
public class FileSystemStorageService implements StorageService {

    private final StorageProperties storageProperties;
    private Path rootLocation;

    public FileSystemStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(storageProperties.getLocal().getBasePath())
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Initialized FileSystemStorageService at root: {}", this.rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directory: " + storageProperties.getLocal().getBasePath(), e);
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    public StoredFileResponse uploadFile(MultipartFile file, String folder) {
        return uploadFile(file, folder, Collections.emptyMap());
    }

    @Override
    public StoredFileResponse uploadFile(MultipartFile file, String folder, Map<String, String> customMetadata) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
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
            throw new StorageException("Failed to store file: InputStream is null");
        }

        String safeFileName = sanitizeFileName(fileName);
        String fileKey = generateFileKey(folder, safeFileName);
        Path destination = resolveAndValidatePath(fileKey);

        try {
            // Ensure parent directories exist
            Path parent = destination.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            // Write content to target file
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            long actualSize = size > 0 ? size : Files.size(destination);

            String detectedContentType = resolveContentType(destination, contentType);
            String url = getFileUrl(fileKey);

            log.info("File successfully stored to filesystem: key={}, size={} bytes", fileKey, actualSize);

            return StoredFileResponse.builder()
                    .fileKey(fileKey)
                    .fileName(safeFileName)
                    .contentType(detectedContentType)
                    .size(actualSize)
                    .url(url)
                    .storageType(StorageType.LOCAL)
                    .uploadedAt(LocalDateTime.now())
                    .metadata(customMetadata != null ? customMetadata : Collections.emptyMap())
                    .build();
        } catch (IOException e) {
            throw new StorageException("Failed to store file to filesystem: " + fileKey, e);
        }
    }

    @Override
    public StoredFileResponse uploadFile(byte[] content, String fileName, String contentType, String folder) {
        if (content == null || content.length == 0) {
            throw new StorageException("Failed to store empty byte array");
        }
        return uploadFile(new ByteArrayInputStream(content), fileName, contentType, content.length, folder);
    }

    @Override
    public Resource downloadFile(String fileKey) {
        Path filePath = resolveAndValidatePath(fileKey);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileNotFoundStorageException("File not found or not readable: " + fileKey);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundStorageException("Could not read file: " + fileKey);
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file URL: " + fileKey, e);
        }
    }

    @Override
    public StorageMetadataResponse getMetadata(String fileKey) {
        Path filePath = resolveAndValidatePath(fileKey);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundStorageException("File does not exist: " + fileKey);
        }

        try {
            long size = Files.size(filePath);
            Instant modifiedInstant = Files.getLastModifiedTime(filePath).toInstant();
            LocalDateTime lastModified = LocalDateTime.ofInstant(modifiedInstant, ZoneId.systemDefault());
            String contentType = resolveContentType(filePath, null);
            String fileName = filePath.getFileName().toString();

            return StorageMetadataResponse.builder()
                    .fileKey(fileKey)
                    .fileName(fileName)
                    .contentType(contentType)
                    .size(size)
                    .storageType(StorageType.LOCAL)
                    .lastModified(lastModified)
                    .eTag(String.valueOf(modifiedInstant.toEpochMilli()))
                    .customMetadata(Collections.emptyMap())
                    .build();
        } catch (IOException e) {
            throw new StorageException("Failed to retrieve file metadata for: " + fileKey, e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        Path filePath = resolveAndValidatePath(fileKey);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File successfully deleted from filesystem: {}", fileKey);
            } else {
                log.warn("File was not found for deletion: {}", fileKey);
                throw new FileNotFoundStorageException("File not found for deletion: " + fileKey);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to delete file from filesystem: " + fileKey, e);
        }
    }

    @Override
    public String getFileUrl(String fileKey) {
        String baseUrl = storageProperties.getLocal().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "/api/storage/files";
        }
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanKey = fileKey.startsWith("/") ? fileKey.substring(1) : fileKey;
        return cleanBase + "/" + cleanKey;
    }

    @Override
    public String generatePresignedUrl(String fileKey, Duration expiration) {
        // Local storage provides direct endpoint URL
        return getFileUrl(fileKey);
    }

    @Override
    public List<StorageMetadataResponse> listFiles(String folderOrPrefix) {
        Path targetDir = (folderOrPrefix != null && !folderOrPrefix.isBlank())
                ? resolveAndValidatePath(folderOrPrefix)
                : this.rootLocation;

        if (!Files.exists(targetDir)) {
            return Collections.emptyList();
        }

        List<StorageMetadataResponse> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(targetDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relativeKey = this.rootLocation.relativize(path).toString().replace("\\", "/");
                        try {
                            results.add(getMetadata(relativeKey));
                        } catch (Exception ex) {
                            log.warn("Error reading metadata for listed file: {}", path, ex);
                        }
                    });
        } catch (IOException e) {
            throw new StorageException("Failed to list files under: " + folderOrPrefix, e);
        }
        return results;
    }

    @Override
    public boolean exists(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return false;
        }
        try {
            Path path = resolveAndValidatePath(fileKey);
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (StorageException e) {
            return false;
        }
    }

    /**
     * Resolves and verifies that the fileKey does not escape the configured root directory.
     */
    public Path resolveAndValidatePath(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new StorageException("File key cannot be null or empty");
        }
        if (fileKey.contains("..")) {
            throw new StorageException("File key contains illegal relative path traversal sequence '..': " + fileKey);
        }

        // Strip leading slashes to prevent escaping root directory on Windows and Unix
        String cleanKey = fileKey.replaceAll("^[\\\\/]+", "");
        Path resolved = this.rootLocation.resolve(cleanKey).normalize().toAbsolutePath();
        if (!resolved.startsWith(this.rootLocation)) {
            throw new StorageException("Access denied: File key outside storage root: " + fileKey);
        }
        return resolved;
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
        // Replace spaces and special characters with underscores, preserving extension
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

    private String resolveContentType(Path path, String fallbackContentType) {
        try {
            String probed = Files.probeContentType(path);
            if (StringUtils.hasText(probed)) {
                return probed;
            }
        } catch (IOException ignored) {}

        if (StringUtils.hasText(fallbackContentType)) {
            return fallbackContentType;
        }

        return MediaTypeFactory.getMediaType(path.getFileName().toString())
                .map(Object::toString)
                .orElse("application/octet-stream");
    }
}
