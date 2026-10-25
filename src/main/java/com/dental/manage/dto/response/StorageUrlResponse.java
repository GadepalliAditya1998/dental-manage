package com.dental.manage.dto.response;

import com.dental.manage.enums.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for generated direct or presigned storage URLs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUrlResponse {

    /**
     * File key in storage.
     */
    private String fileKey;

    /**
     * Generated direct or pre-signed URL.
     */
    private String url;

    /**
     * Storage provider type.
     */
    private StorageType storageType;

    /**
     * Presigned expiration time (null for permanent or local URLs).
     */
    private LocalDateTime expiresAt;
}
