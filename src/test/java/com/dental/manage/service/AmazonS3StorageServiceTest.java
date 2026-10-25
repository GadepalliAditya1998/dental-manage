package com.dental.manage.service;

import com.dental.manage.config.StorageProperties;
import com.dental.manage.dto.response.StorageMetadataResponse;
import com.dental.manage.dto.response.StoredFileResponse;
import com.dental.manage.enums.StorageType;
import com.dental.manage.exception.FileNotFoundStorageException;
import com.dental.manage.service.impl.AmazonS3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmazonS3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private AmazonS3StorageService s3StorageService;
    private StorageProperties storageProperties;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.getS3().setBucket("test-dental-bucket");
        storageProperties.getS3().setRegion("us-east-1");
        storageProperties.getS3().setAccessKey("TESTACCESSKEY");
        storageProperties.getS3().setSecretKey("TESTSECRETKEY");

        s3StorageService = new AmazonS3StorageService(storageProperties, s3Client, s3Presigner);
    }

    @Test
    @DisplayName("Should return S3 storage type")
    void shouldReturnS3StorageType() {
        assertThat(s3StorageService.getStorageType()).isEqualTo(StorageType.S3);
    }

    @Test
    @DisplayName("Should successfully upload multipart file to S3")
    void shouldUploadMultipartFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "xray.png",
                "image/png",
                "dummy-png-data".getBytes(StandardCharsets.UTF_8)
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        StoredFileResponse response = s3StorageService.uploadFile(file, "patients/10", Map.of("patientId", "10"));

        assertThat(response).isNotNull();
        assertThat(response.getFileKey()).startsWith("patients/10/");
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getStorageType()).isEqualTo(StorageType.S3);
        assertThat(response.getUrl()).contains("test-dental-bucket.s3.us-east-1.amazonaws.com");
        assertThat(response.getMetadata()).containsEntry("patientId", "10");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should successfully download file from S3")
    void shouldDownloadFile() throws IOException {
        String fileKey = "documents/test.pdf";
        byte[] expectedContent = "PDF file content".getBytes(StandardCharsets.UTF_8);

        GetObjectResponse getResponse = GetObjectResponse.builder()
                .contentType("application/pdf")
                .contentLength((long) expectedContent.length)
                .build();

        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
                getResponse,
                AbortableInputStream.create(new ByteArrayInputStream(expectedContent))
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        Resource resource = s3StorageService.downloadFile(fileKey);

        assertThat(resource).isNotNull();
        assertThat(resource.getInputStream().readAllBytes()).isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("Should throw FileNotFoundStorageException when file does not exist in S3")
    void shouldThrowWhenFileNotFoundInS3() {
        String fileKey = "missing.txt";
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("The specified key does not exist.").build());

        assertThatThrownBy(() -> s3StorageService.downloadFile(fileKey))
                .isInstanceOf(FileNotFoundStorageException.class)
                .hasMessageContaining("File not found in S3 bucket");
    }

    @Test
    @DisplayName("Should retrieve object metadata from S3")
    void shouldRetrieveMetadataFromS3() {
        String fileKey = "reports/lab-report.pdf";
        Instant now = Instant.now();

        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(1024L)
                .contentType("application/pdf")
                .lastModified(now)
                .eTag("\"123456\"")
                .metadata(Map.of("category", "lab"))
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        StorageMetadataResponse metadata = s3StorageService.getMetadata(fileKey);

        assertThat(metadata).isNotNull();
        assertThat(metadata.getFileKey()).isEqualTo(fileKey);
        assertThat(metadata.getSize()).isEqualTo(1024L);
        assertThat(metadata.getContentType()).isEqualTo("application/pdf");
        assertThat(metadata.getStorageType()).isEqualTo(StorageType.S3);
        assertThat(metadata.getETag()).isEqualTo("\"123456\"");
        assertThat(metadata.getCustomMetadata()).containsEntry("category", "lab");
    }

    @Test
    @DisplayName("Should check existence in S3 correctly")
    void shouldCheckFileExistenceInS3() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertThat(s3StorageService.exists("valid.txt")).isTrue();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        assertThat(s3StorageService.exists("missing.txt")).isFalse();
    }

    @Test
    @DisplayName("Should delete object from S3")
    void shouldDeleteObjectFromS3() {
        String fileKey = "to-delete.png";

        // Exists check before delete
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        s3StorageService.deleteFile(fileKey);

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Should generate pre-signed URL")
    void shouldGeneratePresignedUrl() throws Exception {
        String fileKey = "secure/patient-history.pdf";
        URI expectedUri = new URI("https://test-dental-bucket.s3.us-east-1.amazonaws.com/secure/patient-history.pdf?X-Amz-Signature=xyz");

        when(presignedGetObjectRequest.url()).thenReturn(expectedUri.toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        String presignedUrl = s3StorageService.generatePresignedUrl(fileKey, Duration.ofMinutes(20));

        assertThat(presignedUrl).isEqualTo(expectedUri.toString());
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should list objects from S3 bucket")
    void shouldListObjectsFromS3() {
        S3Object obj1 = S3Object.builder().key("invoices/1.pdf").size(500L).lastModified(Instant.now()).build();
        S3Object obj2 = S3Object.builder().key("invoices/2.pdf").size(800L).lastModified(Instant.now()).build();

        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(List.of(obj1, obj2))
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

        List<StorageMetadataResponse> files = s3StorageService.listFiles("invoices");

        assertThat(files).hasSize(2);
        assertThat(files.get(0).getFileKey()).isEqualTo("invoices/1.pdf");
        assertThat(files.get(1).getFileKey()).isEqualTo("invoices/2.pdf");
    }

    @Test
    @DisplayName("Should confirm S3 is configured")
    void shouldConfirmConfigured() {
        assertThat(s3StorageService.isConfigured()).isTrue();
    }
}
