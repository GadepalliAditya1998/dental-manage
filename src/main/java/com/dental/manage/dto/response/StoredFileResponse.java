package com.dental.manage.dto.response;

import com.dental.manage.enums.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO returned after a successful file upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFileResponse {

    /**
     * Unique key/path identifier of the file in storage.
     */
    private String fileKey;

    /**
     * Sanitized or original file name.
     */
    private String fileName;

    /**
     * MIME type of the uploaded file.
     */
    private String contentType;

    /**
     * File size in bytes.
     */
    private long size;

    /**
     * Access or download URL for the file.
     */
    private String url;

    /**
     * Storage provider type that stores the file.
     */
    private StorageType storageType;

    /**
     * Timestamp when the file was uploaded.
     */
    private LocalDateTime uploadedAt;

    /**
     * Optional custom metadata key-value pairs.
     */
    private Map<String, String> metadata;
}
