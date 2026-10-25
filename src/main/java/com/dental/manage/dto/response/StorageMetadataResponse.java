package com.dental.manage.dto.response;

import com.dental.manage.enums.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Metadata information for a stored file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageMetadataResponse {

    /**
     * Unique key/path identifier of the file.
     */
    private String fileKey;

    /**
     * File name.
     */
    private String fileName;

    /**
     * MIME type.
     */
    private String contentType;

    /**
     * Size in bytes.
     */
    private long size;

    /**
     * Storage provider where the file is stored.
     */
    private StorageType storageType;

    /**
     * Last modified timestamp.
     */
    private LocalDateTime lastModified;

    /**
     * ETag or checksum (if supported).
     */
    private String eTag;

    /**
     * Custom user-defined metadata.
     */
    private Map<String, String> customMetadata;
}
