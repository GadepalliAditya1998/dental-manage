package com.dental.manage.service;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.enums.StorageType;
import com.dental.manage.service.impl.AmazonS3StorageService;
import com.dental.manage.service.impl.FileSystemStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class StorageServiceFactoryTest {

    @Mock
    private FileSystemStorageService fileSystemStorageService;

    @Mock
    private AmazonS3StorageService amazonS3StorageService;

    private StorageProperties storageProperties;
    private StorageServiceFactory factory;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        factory = new StorageServiceFactory(storageProperties, fileSystemStorageService, amazonS3StorageService);
    }

    @Test
    @DisplayName("Should resolve FileSystemStorageService when LOCAL is requested")
    void shouldResolveFileSystemStorageService() {
        StorageService service = factory.getService(StorageType.LOCAL);
        assertThat(service).isSameAs(fileSystemStorageService);
    }

    @Test
    @DisplayName("Should resolve AmazonS3StorageService when S3 is requested")
    void shouldResolveAmazonS3StorageService() {
        StorageService service = factory.getService(StorageType.S3);
        assertThat(service).isSameAs(amazonS3StorageService);
    }

    @Test
    @DisplayName("Should resolve service by string case-insensitively")
    void shouldResolveByString() {
        assertThat(factory.getService("local")).isSameAs(fileSystemStorageService);
        assertThat(factory.getService("LOCAL")).isSameAs(fileSystemStorageService);
        assertThat(factory.getService("filesystem")).isSameAs(fileSystemStorageService);

        assertThat(factory.getService("s3")).isSameAs(amazonS3StorageService);
        assertThat(factory.getService("S3")).isSameAs(amazonS3StorageService);
        assertThat(factory.getService("amazonS3")).isSameAs(amazonS3StorageService);
    }

    @Test
    @DisplayName("Should return default service when null or empty provider requested")
    void shouldReturnDefaultServiceForNullOrEmpty() {
        storageProperties.setProvider(StorageType.LOCAL);
        assertThat(factory.getService((StorageType) null)).isSameAs(fileSystemStorageService);
        assertThat(factory.getService("")).isSameAs(fileSystemStorageService);
        assertThat(factory.getService("   ")).isSameAs(fileSystemStorageService);

        storageProperties.setProvider(StorageType.S3);
        assertThat(factory.getDefaultService()).isSameAs(amazonS3StorageService);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unknown provider string")
    void shouldThrowForUnknownProvider() {
        assertThatThrownBy(() -> factory.getService("google-cloud"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown storage provider");
    }

    @Test
    @DisplayName("Should report all available providers")
    void shouldReportAvailableProviders() {
        assertThat(factory.getAvailableProviders()).containsExactlyInAnyOrder(StorageType.LOCAL, StorageType.S3);
    }
}
