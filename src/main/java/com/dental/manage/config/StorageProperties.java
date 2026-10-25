package com.dental.manage.config;

import com.dental.manage.enums.StorageType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for file storage backends (Local FileSystem and Amazon S3).
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * Default storage provider type (LOCAL or S3).
     */
    private StorageType provider = StorageType.LOCAL;

    /**
     * Local filesystem storage properties.
     */
    private LocalProperties local = new LocalProperties();

    /**
     * Amazon S3 storage properties.
     */
    private S3Properties s3 = new S3Properties();

    @Data
    public static class LocalProperties {
        /**
         * Base directory path for storing files locally on the server.
         */
        private String basePath = "./uploads";

        /**
         * Base URL prefix for accessing locally stored files.
         */
        private String baseUrl = "http://localhost:8080/api/storage/files";
    }

    @Data
    public static class S3Properties {
        /**
         * S3 bucket name.
         */
        private String bucket = "dental-clinic-storage";

        /**
         * AWS region (e.g., us-east-1, ap-south-1).
         */
        private String region = "us-east-1";

        /**
         * AWS access key ID (optional if using IAM role / environment variables).
         */
        private String accessKey;

        /**
         * AWS secret access key (optional if using IAM role / environment variables).
         */
        private String secretKey;

        /**
         * Custom S3 endpoint URL (optional, useful for LocalStack, MinIO, or custom S3-compatible storage).
         */
        private String endpoint;

        /**
         * Whether to enable path-style access (useful for MinIO or LocalStack).
         */
        private boolean pathStyleAccess = false;

        /**
         * Expiration time in minutes for pre-signed URLs.
         */
        private int presignedUrlExpirationMinutes = 15;
    }
}
