package com.dental.manage.service;

import com.dental.manage.dto.response.StorageMetadataResponse;
import com.dental.manage.dto.response.StoredFileResponse;
import com.dental.manage.enums.StorageType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Generic Storage Service interface defining file storage operations.
 * Concrete implementations provide support for Local FileSystem and Amazon S3 buckets.
 */
public interface StorageService {

    /**
     * Identifies the underlying storage provider type.
     *
     * @return StorageType (LOCAL or S3)
     */
    StorageType getStorageType();

    /**
     * Uploads a multipart file to storage.
     *
     * @param file   The multipart file to upload
     * @param folder Optional directory or folder prefix
     * @return StoredFileResponse containing metadata and access URL
     */
    StoredFileResponse uploadFile(MultipartFile file, String folder);

    /**
     * Uploads a multipart file to storage with custom metadata.
     *
     * @param file           The multipart file to upload
     * @param folder         Optional directory or folder prefix
     * @param customMetadata Additional metadata key-value pairs
     * @return StoredFileResponse containing metadata and access URL
     */
    StoredFileResponse uploadFile(MultipartFile file, String folder, Map<String, String> customMetadata);

    /**
     * Uploads an input stream to storage.
     *
     * @param inputStream The data stream
     * @param fileName    Target file name
     * @param contentType MIME type (e.g. image/png, application/pdf)
     * @param size        Content length in bytes
     * @param folder      Optional directory or folder prefix
     * @return StoredFileResponse containing metadata and access URL
     */
    StoredFileResponse uploadFile(InputStream inputStream, String fileName, String contentType, long size, String folder);

    /**
     * Uploads an input stream to storage with custom metadata.
     *
     * @param inputStream    The data stream
     * @param fileName       Target file name
     * @param contentType    MIME type
     * @param size           Content length in bytes
     * @param folder         Optional directory or folder prefix
     * @param customMetadata Additional metadata key-value pairs
     * @return StoredFileResponse containing metadata and access URL
     */
    StoredFileResponse uploadFile(InputStream inputStream, String fileName, String contentType, long size, String folder, Map<String, String> customMetadata);

    /**
     * Uploads byte array content to storage.
     *
     * @param content     Raw file bytes
     * @param fileName    Target file name
     * @param contentType MIME type
     * @param folder      Optional directory or folder prefix
     * @return StoredFileResponse containing metadata and access URL
     */
    StoredFileResponse uploadFile(byte[] content, String fileName, String contentType, String folder);

    /**
     * Downloads a file from storage as a Spring Resource.
     *
     * @param fileKey Unique key/path of the file in storage
     * @return Spring Resource streamable to client
     */
    Resource downloadFile(String fileKey);

    /**
     * Retrieves metadata for a file in storage.
     *
     * @param fileKey Unique key/path of the file
     * @return StorageMetadataResponse
     */
    StorageMetadataResponse getMetadata(String fileKey);

    /**
     * Deletes a file from storage.
     *
     * @param fileKey Unique key/path of the file
     */
    void deleteFile(String fileKey);

    /**
     * Generates a direct or public retrieval URL for the file.
     *
     * @param fileKey Unique key/path of the file
     * @return URL string
     */
    String getFileUrl(String fileKey);

    /**
     * Generates a pre-signed URL with specified expiration duration.
     *
     * @param fileKey    Unique key/path of the file
     * @param expiration Expiration duration
     * @return Pre-signed URL string
     */
    String generatePresignedUrl(String fileKey, Duration expiration);

    /**
     * Lists files under the specified folder or prefix.
     *
     * @param folderOrPrefix Directory or prefix to filter
     * @return List of StorageMetadataResponse
     */
    List<StorageMetadataResponse> listFiles(String folderOrPrefix);

    /**
     * Checks if a file exists in storage.
     *
     * @param fileKey Unique key/path of the file
     * @return true if file exists, false otherwise
     */
    boolean exists(String fileKey);
}
