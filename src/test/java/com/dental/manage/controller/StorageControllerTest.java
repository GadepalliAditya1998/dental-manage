package com.dental.manage.controller;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.dto.response.StorageMetadataResponse;
import com.dental.manage.dto.response.StoredFileResponse;
import com.dental.manage.enums.StorageType;
import com.dental.manage.exception.FileNotFoundStorageException;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.service.StorageService;
import com.dental.manage.service.StorageServiceFactory;
import com.dental.manage.service.impl.AmazonS3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StorageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StorageServiceFactory storageServiceFactory;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private AmazonS3StorageService amazonS3StorageService;

    @Mock
    private StorageService mockStorageService;

    @InjectMocks
    private StorageController storageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/storage/upload should upload file and return 201 Created")
    void shouldUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "teeth-xray.png", "image/png", "fake-image".getBytes()
        );

        StoredFileResponse fileResponse = StoredFileResponse.builder()
                .fileKey("xrays/teeth-xray.png")
                .fileName("teeth-xray.png")
                .contentType("image/png")
                .size(10L)
                .url("http://localhost:8080/api/storage/files/xrays/teeth-xray.png")
                .storageType(StorageType.LOCAL)
                .uploadedAt(LocalDateTime.now())
                .metadata(Collections.emptyMap())
                .build();

        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        when(mockStorageService.getStorageType()).thenReturn(StorageType.LOCAL);
        when(mockStorageService.uploadFile(any(), eq("xrays"))).thenReturn(fileResponse);

        mockMvc.perform(multipart("/api/storage/upload")
                        .file(file)
                        .param("folder", "xrays"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileKey", is("xrays/teeth-xray.png")))
                .andExpect(jsonPath("$.data.storageType", is("local")));

        verify(mockStorageService).uploadFile(any(), eq("xrays"));
    }

    @Test
    @DisplayName("POST /api/storage/upload/batch should upload multiple files and return 201 Created")
    void shouldUploadBatchFiles() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.txt", "text/plain", "data1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "file2.txt", "text/plain", "data2".getBytes());

        StoredFileResponse response1 = StoredFileResponse.builder()
                .fileKey("batch/file1.txt")
                .storageType(StorageType.LOCAL)
                .build();
        StoredFileResponse response2 = StoredFileResponse.builder()
                .fileKey("batch/file2.txt")
                .storageType(StorageType.LOCAL)
                .build();

        when(storageServiceFactory.getService(eq("local"))).thenReturn(mockStorageService);
        when(mockStorageService.getStorageType()).thenReturn(StorageType.LOCAL);
        when(mockStorageService.uploadFile(any(), eq("batch")))
                .thenReturn(response1)
                .thenReturn(response2);

        mockMvc.perform(multipart("/api/storage/upload/batch")
                        .file(file1)
                        .file(file2)
                        .param("folder", "batch")
                        .param("provider", "local"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].fileKey", is("batch/file1.txt")))
                .andExpect(jsonPath("$.data[1].fileKey", is("batch/file2.txt")));
    }

    @Test
    @DisplayName("GET /api/storage/download should return file as attachment")
    void shouldDownloadFile() throws Exception {
        byte[] content = "Hello Download".getBytes(StandardCharsets.UTF_8);
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "patient-report.pdf";
            }
        };

        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        when(mockStorageService.downloadFile("records/patient-report.pdf")).thenReturn(resource);

        mockMvc.perform(get("/api/storage/download")
                        .param("fileKey", "records/patient-report.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"patient-report.pdf\""))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("GET /api/storage/view should return file inline")
    void shouldViewFileInline() throws Exception {
        byte[] content = "image-bytes".getBytes(StandardCharsets.UTF_8);
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "scan.png";
            }
        };

        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        when(mockStorageService.downloadFile("scans/scan.png")).thenReturn(resource);

        mockMvc.perform(get("/api/storage/view")
                        .param("fileKey", "scans/scan.png"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"scan.png\""))
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @DisplayName("GET /api/storage/files/** should serve local files directly with URL decoding")
    void shouldServeLocalFileWithUrlDecoding() throws Exception {
        byte[] content = "file-bytes".getBytes(StandardCharsets.UTF_8);
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "patient xray.png";
            }
        };

        when(storageServiceFactory.getService(StorageType.LOCAL)).thenReturn(mockStorageService);
        when(mockStorageService.downloadFile("xrays/patient xray.png")).thenReturn(resource);

        mockMvc.perform(get(java.net.URI.create("/api/storage/files/xrays/patient%20xray.png")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"patient xray.png\""))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("GET /api/storage/metadata should return file metadata")
    void shouldGetFileMetadata() throws Exception {
        StorageMetadataResponse metadata = StorageMetadataResponse.builder()
                .fileKey("test.pdf")
                .fileName("test.pdf")
                .contentType("application/pdf")
                .size(2048L)
                .storageType(StorageType.LOCAL)
                .lastModified(LocalDateTime.of(2026, 10, 24, 10, 0))
                .eTag("etag-123")
                .build();

        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        when(mockStorageService.getMetadata("test.pdf")).thenReturn(metadata);

        mockMvc.perform(get("/api/storage/metadata")
                        .param("fileKey", "test.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileKey", is("test.pdf")))
                .andExpect(jsonPath("$.data.size", is(2048)))
                .andExpect(jsonPath("$.data.contentType", is("application/pdf")));
    }

    @Test
    @DisplayName("GET /api/storage/url should return generated pre-signed or direct URL")
    void shouldGetFileUrl() throws Exception {
        when(storageServiceFactory.getService(eq("s3"))).thenReturn(mockStorageService);
        when(mockStorageService.generatePresignedUrl(eq("reports/1.pdf"), eq(Duration.ofMinutes(15))))
                .thenReturn("https://bucket.s3.amazonaws.com/reports/1.pdf?signature=xyz");
        when(mockStorageService.getStorageType()).thenReturn(StorageType.S3);

        mockMvc.perform(get("/api/storage/url")
                        .param("fileKey", "reports/1.pdf")
                        .param("expirationMinutes", "15")
                        .param("provider", "s3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.url", containsString("signature=xyz")))
                .andExpect(jsonPath("$.data.storageType", is("s3")));
    }

    @Test
    @DisplayName("GET /api/storage/list should return list of files")
    void shouldListFiles() throws Exception {
        StorageMetadataResponse meta1 = StorageMetadataResponse.builder()
                .fileKey("documents/a.pdf")
                .fileName("a.pdf")
                .storageType(StorageType.LOCAL)
                .build();

        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        when(mockStorageService.listFiles("documents")).thenReturn(List.of(meta1));

        mockMvc.perform(get("/api/storage/list")
                        .param("folder", "documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fileKey", is("documents/a.pdf")));
    }

    @Test
    @DisplayName("GET /api/storage/exists should check existence")
    void shouldCheckExists() throws Exception {
        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        when(mockStorageService.exists("file.txt")).thenReturn(true);

        mockMvc.perform(get("/api/storage/exists")
                        .param("fileKey", "file.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/storage should delete file and return success")
    void shouldDeleteFile() throws Exception {
        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        doNothing().when(mockStorageService).deleteFile("file.txt");

        mockMvc.perform(delete("/api/storage")
                        .param("fileKey", "file.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("File deleted successfully")));

        verify(mockStorageService).deleteFile("file.txt");
    }

    @Test
    @DisplayName("GET /api/storage/download should return 404 when FileNotFoundStorageException occurs")
    void shouldReturn404WhenFileNotFound() throws Exception {
        when(storageServiceFactory.getService((String) null)).thenReturn(mockStorageService);
        when(mockStorageService.downloadFile("missing.pdf"))
                .thenThrow(new FileNotFoundStorageException("File not found: missing.pdf"));

        mockMvc.perform(get("/api/storage/download")
                        .param("fileKey", "missing.pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("File not found: missing.pdf")));
    }

    @Test
    @DisplayName("GET /api/storage/info should return storage system status")
    void shouldGetStorageInfo() throws Exception {
        StorageProperties.LocalProperties local = new StorageProperties.LocalProperties();
        local.setBasePath("./uploads");

        StorageProperties.S3Properties s3 = new StorageProperties.S3Properties();
        s3.setBucket("dental-clinic-storage");
        s3.setRegion("us-east-1");

        when(storageProperties.getProvider()).thenReturn(StorageType.LOCAL);
        when(storageProperties.getLocal()).thenReturn(local);
        when(storageProperties.getS3()).thenReturn(s3);
        when(storageServiceFactory.getAvailableProviders()).thenReturn(Set.of(StorageType.LOCAL, StorageType.S3));
        when(amazonS3StorageService.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/storage/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.activeProvider", is("local")))
                .andExpect(jsonPath("$.data.localBasePath", is("./uploads")))
                .andExpect(jsonPath("$.data.s3Bucket", is("dental-clinic-storage")))
                .andExpect(jsonPath("$.data.s3Configured", is(true)));
    }
}
