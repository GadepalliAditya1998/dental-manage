package com.dental.manage.controller;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.StorageInfoResponse;
import com.dental.manage.dto.response.StorageMetadataResponse;
import com.dental.manage.dto.response.StorageUrlResponse;
import com.dental.manage.dto.response.StoredFileResponse;
import com.dental.manage.enums.StorageType;
import com.dental.manage.service.StorageService;
import com.dental.manage.service.StorageServiceFactory;
import com.dental.manage.service.impl.AmazonS3StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated REST Controller for Storage operations.
 * Supports FileStorage (local) and Amazon S3 backends interchangeably.
 */
@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageServiceFactory storageServiceFactory;
    private final StorageProperties storageProperties;
    private final AmazonS3StorageService amazonS3StorageService;

    /**
     * Uploads a single file.
     *
     * @param file     Multipart file to store
     * @param folder   Optional directory or prefix in storage
     * @param provider Optional storage provider (local, s3). Defaults to active provider if omitted.
     * @return ApiResponse containing StoredFileResponse
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoredFileResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        log.info("Uploading file '{}' via provider '{}'", file.getOriginalFilename(), service.getStorageType());

        StoredFileResponse response = service.uploadFile(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", response));
    }

    /**
     * Uploads multiple files in batch.
     *
     * @param files    List of multipart files to store
     * @param folder   Optional directory or prefix in storage
     * @param provider Optional storage provider (local, s3). Defaults to active provider if omitted.
     * @return ApiResponse containing list of StoredFileResponse
     */
    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<StoredFileResponse>>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", required = false) String folder,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        log.info("Batch uploading {} files via provider '{}'", files.size(), service.getStorageType());

        List<StoredFileResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(service.uploadFile(file, folder));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch files uploaded successfully", responses));
    }

    /**
     * Downloads a file as an attachment.
     *
     * @param fileKey  Storage key/path of the file
     * @param provider Optional storage provider (local, s3)
     * @return Streamable Resource with Content-Disposition attachment
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("fileKey") String fileKey,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        Resource resource = service.downloadFile(fileKey);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "download";
        MediaType mediaType = resolveMediaType(fileName);

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

        try {
            long contentLength = resource.contentLength();
            if (contentLength > 0) {
                responseBuilder.contentLength(contentLength);
            }
        } catch (IOException ignored) {}

        return responseBuilder.body(resource);
    }

    /**
     * Views or streams a file inline (e.g. for display in browser/viewer).
     *
     * @param fileKey  Storage key/path of the file
     * @param provider Optional storage provider (local, s3)
     * @return Streamable Resource with Content-Disposition inline
     */
    @GetMapping("/view")
    public ResponseEntity<Resource> viewFile(
            @RequestParam("fileKey") String fileKey,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        Resource resource = service.downloadFile(fileKey);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "file";
        MediaType mediaType = resolveMediaType(fileName);

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");

        try {
            long contentLength = resource.contentLength();
            if (contentLength > 0) {
                responseBuilder.contentLength(contentLength);
            }
        } catch (IOException ignored) {}

        return responseBuilder.body(resource);
    }

    /**
     * Direct file streaming endpoint for local storage files via wildcard path.
     * Example: GET /api/storage/files/dental-xrays/scan.png
     */
    @GetMapping("/files/**")
    public ResponseEntity<Resource> serveLocalFile(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String prefix = "/api/storage/files/";
        int index = requestUri.indexOf(prefix);
        if (index == -1) {
            return ResponseEntity.badRequest().build();
        }

        String rawKey = requestUri.substring(index + prefix.length());
        String fileKey = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
        StorageService service = storageServiceFactory.getService(StorageType.LOCAL);
        Resource resource = service.downloadFile(fileKey);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "file";
        MediaType mediaType = resolveMediaType(fileName);

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");

        try {
            long contentLength = resource.contentLength();
            if (contentLength > 0) {
                responseBuilder.contentLength(contentLength);
            }
        } catch (IOException ignored) {}

        return responseBuilder.body(resource);
    }

    /**
     * Retrieves file metadata.
     *
     * @param fileKey  Storage key/path of the file
     * @param provider Optional storage provider (local, s3)
     * @return StorageMetadataResponse
     */
    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<StorageMetadataResponse>> getMetadata(
            @RequestParam("fileKey") String fileKey,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        StorageMetadataResponse metadata = service.getMetadata(fileKey);
        return ResponseEntity.ok(ApiResponse.success("File metadata retrieved successfully", metadata));
    }

    /**
     * Generates a direct or pre-signed URL for the file.
     *
     * @param fileKey           Storage key/path of the file
     * @param expirationMinutes Pre-signed URL validity duration in minutes (default 15)
     * @param provider          Optional storage provider (local, s3)
     * @return StorageUrlResponse
     */
    @GetMapping("/url")
    public ResponseEntity<ApiResponse<StorageUrlResponse>> getFileUrl(
            @RequestParam("fileKey") String fileKey,
            @RequestParam(value = "expirationMinutes", required = false, defaultValue = "15") int expirationMinutes,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        String url = service.generatePresignedUrl(fileKey, Duration.ofMinutes(expirationMinutes));

        StorageUrlResponse response = StorageUrlResponse.builder()
                .fileKey(fileKey)
                .url(url)
                .storageType(service.getStorageType())
                .expiresAt(service.getStorageType() == StorageType.S3 ? LocalDateTime.now().plusMinutes(expirationMinutes) : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success("URL generated successfully", response));
    }

    /**
     * Lists files under a given directory or prefix.
     *
     * @param folder   Target directory/prefix
     * @param provider Optional storage provider (local, s3)
     * @return List of StorageMetadataResponse
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<StorageMetadataResponse>>> listFiles(
            @RequestParam(value = "folder", required = false, defaultValue = "") String folder,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        List<StorageMetadataResponse> list = service.listFiles(folder);
        return ResponseEntity.ok(ApiResponse.success("Files listed successfully", list));
    }

    /**
     * Checks if a file exists in storage.
     *
     * @param fileKey  Storage key/path of the file
     * @param provider Optional storage provider (local, s3)
     * @return boolean true if file exists, false otherwise
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> exists(
            @RequestParam("fileKey") String fileKey,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        boolean fileExists = service.exists(fileKey);
        return ResponseEntity.ok(ApiResponse.success("File existence check completed", fileExists));
    }

    /**
     * Deletes a file from storage.
     *
     * @param fileKey  Storage key/path of the file
     * @param provider Optional storage provider (local, s3)
     * @return ApiResponse
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("fileKey") String fileKey,
            @RequestParam(value = "provider", required = false) String provider) {

        StorageService service = storageServiceFactory.getService(provider);
        service.deleteFile(fileKey);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Retrieves diagnostic and configuration info about active and available storage backends.
     *
     * @return StorageInfoResponse
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<StorageInfoResponse>> getStorageInfo() {
        StorageInfoResponse info = StorageInfoResponse.builder()
                .activeProvider(storageProperties.getProvider())
                .supportedProviders(new ArrayList<>(storageServiceFactory.getAvailableProviders()))
                .localBasePath(storageProperties.getLocal().getBasePath())
                .s3Bucket(storageProperties.getS3().getBucket())
                .s3Region(storageProperties.getS3().getRegion())
                .s3Configured(amazonS3StorageService.isConfigured())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Storage system information retrieved", info));
    }

    private MediaType resolveMediaType(String fileName) {
        return MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
