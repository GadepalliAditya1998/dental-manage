package com.dental.manage.dto.response;

import com.dental.manage.enums.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Diagnostic and status information about active and supported storage backends.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageInfoResponse {

    /**
     * Currently active default storage provider.
     */
    private StorageType activeProvider;

    /**
     * All available storage provider implementations.
     */
    private List<StorageType> supportedProviders;

    /**
     * Local storage base directory path.
     */
    private String localBasePath;

    /**
     * Amazon S3 bucket name.
     */
    private String s3Bucket;

    /**
     * Amazon S3 region.
     */
    private String s3Region;

    /**
     * Whether S3 credentials and bucket are configured.
     */
    private boolean s3Configured;
}
