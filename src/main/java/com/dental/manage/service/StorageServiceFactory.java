package com.dental.manage.service;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.enums.StorageType;
import com.dental.manage.service.impl.AmazonS3StorageService;
import com.dental.manage.service.impl.FileSystemStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory and Strategy resolver for StorageService implementations.
 * Resolves the appropriate storage implementation based on configuration or runtime parameters.
 */
@Slf4j
@Component
public class StorageServiceFactory {

    private final StorageProperties storageProperties;
    private final Map<StorageType, StorageService> serviceMap = new EnumMap<>(StorageType.class);

    public StorageServiceFactory(
            StorageProperties storageProperties,
            FileSystemStorageService fileSystemStorageService,
            AmazonS3StorageService amazonS3StorageService) {
        this.storageProperties = storageProperties;
        this.serviceMap.put(StorageType.LOCAL, fileSystemStorageService);
        this.serviceMap.put(StorageType.S3, amazonS3StorageService);
        log.info("Initialized StorageServiceFactory with providers: {}", serviceMap.keySet());
    }

    /**
     * Retrieves the storage service for the requested StorageType.
     *
     * @param storageType Target storage type
     * @return StorageService implementation
     */
    public StorageService getService(StorageType storageType) {
        if (storageType == null) {
            return getDefaultService();
        }
        StorageService service = serviceMap.get(storageType);
        if (service == null) {
            throw new IllegalArgumentException("No storage service registered for provider type: " + storageType);
        }
        return service;
    }

    /**
     * Resolves the storage service by name or string representation.
     * Falls back to default configured provider if string is blank or null.
     *
     * @param providerName Provider name (e.g., "local", "s3")
     * @return StorageService implementation
     */
    public StorageService getService(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return getDefaultService();
        }
        StorageType type = StorageType.fromString(providerName);
        return getService(type);
    }

    /**
     * Retrieves the default storage service based on the configured 'storage.provider'.
     *
     * @return StorageService default implementation
     */
    public StorageService getDefaultService() {
        StorageType defaultType = storageProperties.getProvider() != null
                ? storageProperties.getProvider()
                : StorageType.LOCAL;

        StorageService service = serviceMap.get(defaultType);
        if (service == null) {
            log.warn("Configured default storage provider '{}' is not registered. Falling back to LOCAL.", defaultType);
            service = serviceMap.get(StorageType.LOCAL);
        }
        if (service == null) {
            throw new IllegalStateException("No storage service implementation available");
        }
        return service;
    }

    /**
     * Returns the set of all available/registered storage types.
     *
     * @return Unmodifiable set of supported StorageType
     */
    public Set<StorageType> getAvailableProviders() {
        return Collections.unmodifiableSet(serviceMap.keySet());
    }
}
