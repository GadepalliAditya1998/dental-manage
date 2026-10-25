package com.dental.manage.config;

import com.dental.manage.enums.StorageType;
import com.dental.manage.service.StorageService;
import com.dental.manage.service.impl.AmazonS3StorageService;
import com.dental.manage.service.impl.FileSystemStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration that registers the primary StorageService bean based on application properties.
 */
@Slf4j
@Configuration
public class StorageConfig {

    /**
     * Registers the default @Primary StorageService bean matching the active 'storage.provider' setting.
     */
    @Bean
    @Primary
    public StorageService defaultStorageService(
            StorageProperties storageProperties,
            FileSystemStorageService fileSystemStorageService,
            AmazonS3StorageService amazonS3StorageService) {

        StorageType provider = storageProperties.getProvider();
        if (provider == StorageType.S3) {
            log.info("Primary StorageService configured: Amazon S3 (bucket={})", storageProperties.getS3().getBucket());
            return amazonS3StorageService;
        }

        log.info("Primary StorageService configured: Local FileSystem (path={})", storageProperties.getLocal().getBasePath());
        return fileSystemStorageService;
    }
}
