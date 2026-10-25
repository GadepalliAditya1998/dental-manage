package com.dental.manage.service;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.dto.response.StorageMetadataResponse;
import com.dental.manage.dto.response.StoredFileResponse;
import com.dental.manage.enums.StorageType;
import com.dental.manage.exception.FileNotFoundStorageException;
import com.dental.manage.exception.StorageException;
import com.dental.manage.service.impl.FileSystemStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileSystemStorageService storageService;
    private StorageProperties storageProperties;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.getLocal().setBasePath(tempDir.toString());
        storageProperties.getLocal().setBaseUrl("http://localhost:8080/api/storage/files");

        storageService = new FileSystemStorageService(storageProperties);
        storageService.init();
    }

    @Test
    @DisplayName("Should return LOCAL storage type")
    void shouldReturnLocalStorageType() {
        assertThat(storageService.getStorageType()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    @DisplayName("Should store and read multipart file")
    void shouldStoreAndReadMultipartFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Sample PDF content".getBytes(StandardCharsets.UTF_8)
        );

        StoredFileResponse response = storageService.uploadFile(file, "documents");

        assertThat(response).isNotNull();
        assertThat(response.getFileKey()).startsWith("documents/");
        assertThat(response.getFileName()).contains("test-document.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getSize()).isEqualTo("Sample PDF content".length());
        assertThat(response.getStorageType()).isEqualTo(StorageType.LOCAL);

        // Download file
        Resource resource = storageService.downloadFile(response.getFileKey());
        assertThat(resource.exists()).isTrue();
        try (InputStream is = resource.getInputStream()) {
            assertThat(is.readAllBytes()).isEqualTo("Sample PDF content".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("Should store file with custom metadata")
    void shouldStoreFileWithCustomMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "xray.png",
                "image/png",
                "dummy-image-bytes".getBytes(StandardCharsets.UTF_8)
        );

        Map<String, String> metadata = Map.of("patientId", "123", "doctor", "Dr. Smith");
        StoredFileResponse response = storageService.uploadFile(file, "xrays", metadata);

        assertThat(response.getMetadata()).containsEntry("patientId", "123");
        assertThat(response.getMetadata()).containsEntry("doctor", "Dr. Smith");
    }

    @Test
    @DisplayName("Should store file via InputStream")
    void shouldStoreFileViaInputStream() throws IOException {
        byte[] bytes = "Hello Stream".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        StoredFileResponse response = storageService.uploadFile(
                inputStream, "stream-test.txt", "text/plain", bytes.length, "text"
        );

        assertThat(response.getFileKey()).startsWith("text/");
        assertThat(storageService.exists(response.getFileKey())).isTrue();
    }

    @Test
    @DisplayName("Should store file via raw bytes")
    void shouldStoreFileViaByteArray() {
        byte[] bytes = "Hello Bytes".getBytes(StandardCharsets.UTF_8);
        StoredFileResponse response = storageService.uploadFile(bytes, "bytes.txt", "text/plain", "raw");

        assertThat(response.getFileKey()).startsWith("raw/");
        assertThat(storageService.exists(response.getFileKey())).isTrue();
    }

    @Test
    @DisplayName("Should retrieve file metadata correctly")
    void shouldRetrieveFileMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "Dental clinic notes".getBytes(StandardCharsets.UTF_8)
        );

        StoredFileResponse uploadResponse = storageService.uploadFile(file, "notes");
        StorageMetadataResponse metadata = storageService.getMetadata(uploadResponse.getFileKey());

        assertThat(metadata).isNotNull();
        assertThat(metadata.getFileKey()).isEqualTo(uploadResponse.getFileKey());
        assertThat(metadata.getSize()).isEqualTo("Dental clinic notes".length());
        assertThat(metadata.getStorageType()).isEqualTo(StorageType.LOCAL);
        assertThat(metadata.getLastModified()).isNotNull();
    }

    @Test
    @DisplayName("Should delete stored file")
    void shouldDeleteStoredFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to-delete.txt",
                "text/plain",
                "Temporary data".getBytes(StandardCharsets.UTF_8)
        );

        StoredFileResponse uploadResponse = storageService.uploadFile(file, null);
        assertThat(storageService.exists(uploadResponse.getFileKey())).isTrue();

        storageService.deleteFile(uploadResponse.getFileKey());
        assertThat(storageService.exists(uploadResponse.getFileKey())).isFalse();
    }

    @Test
    @DisplayName("Should throw FileNotFoundStorageException when deleting non-existent file")
    void shouldThrowWhenDeletingNonExistentFile() {
        assertThatThrownBy(() -> storageService.deleteFile("non-existent-key.txt"))
                .isInstanceOf(FileNotFoundStorageException.class);
    }

    @Test
    @DisplayName("Should list files under folder")
    void shouldListFilesUnderFolder() {
        MockMultipartFile file1 = new MockMultipartFile("file", "one.txt", "text/plain", "1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "two.txt", "text/plain", "2".getBytes());

        storageService.uploadFile(file1, "reports");
        storageService.uploadFile(file2, "reports");

        List<StorageMetadataResponse> files = storageService.listFiles("reports");
        assertThat(files).hasSize(2);
    }

    @Test
    @DisplayName("Should reject path traversal attempts")
    void shouldRejectPathTraversal() {
        assertThatThrownBy(() -> storageService.resolveAndValidatePath("../escape.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("illegal relative path traversal");

        assertThatThrownBy(() -> storageService.downloadFile("../../etc/passwd"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should reject empty file upload")
    void shouldRejectEmptyFileUpload() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> storageService.uploadFile(emptyFile, "folder"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("empty file");
    }

    @Test
    @DisplayName("Should resolve and store file even when key has leading slashes")
    void shouldHandleLeadingSlashesInKey() {
        MockMultipartFile file = new MockMultipartFile("file", "lead.txt", "text/plain", "data".getBytes());
        StoredFileResponse response = storageService.uploadFile(file, "/folder/sub");
        assertThat(response.getFileKey()).doesNotStartWith("/");
        assertThat(storageService.exists(response.getFileKey())).isTrue();
    }

    @Test
    @DisplayName("Should generate file URL with base URL")
    void shouldGenerateFileUrl() {
        String url = storageService.getFileUrl("patients/123/card.pdf");
        assertThat(url).isEqualTo("http://localhost:8080/api/storage/files/patients/123/card.pdf");

        String presigned = storageService.generatePresignedUrl("patients/123/card.pdf", Duration.ofMinutes(10));
        assertThat(presigned).isEqualTo("http://localhost:8080/api/storage/files/patients/123/card.pdf");
    }
}
