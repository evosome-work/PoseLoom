package com.evosome.poseloombackend.integration;

import com.evosome.poseloombackend.PoseloomApplication;
import com.evosome.poseloombackend.client.S3UploadingClient;
import com.evosome.poseloombackend.config.S3Properties;
import com.evosome.poseloombackend.config.UploadProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PoseloomApplication.class)
@ActiveProfiles("test")
@Testcontainers
class S3ClientIntegrationTests {

    private static final String POSTGRES_IMAGE = System.getenv().getOrDefault("POSTGRES_IMAGE", "postgres:18");

    private static final String MINIO_IMAGE = System.getenv().getOrDefault("MINIO_IMAGE",
            "pgsty/silo:RELEASE.2026-10-26T00-00-00Z");

    private static final String MINIO_USER = System.getenv().getOrDefault("MINIO_USER", "test");

    private static final String MINIO_PASSWORD = System.getenv().getOrDefault("MINIO_PASSWORD", "testtest");

    private static final String PUBLIC_ENDPOINT = System.getenv().getOrDefault("S3_PUBLIC_ENDPOINT",
            "http://minio-public.example:9000");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE));

    @Container
    @SuppressWarnings("resource") // Resource leak: '<unassigned Closeable value>' is never closed
    static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName
                    .parse(MINIO_IMAGE)
                    .asCompatibleSubstituteFor("minio/minio"))
            .withUserName(MINIO_USER)
            .withPassword(MINIO_PASSWORD);

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("poseloom.storage.s3.internal-endpoint", MINIO::getS3URL);
        registry.add("poseloom.storage.s3.public-endpoint", () -> PUBLIC_ENDPOINT);
        registry.add("poseloom.storage.s3.access-key", MINIO::getUserName);
        registry.add("poseloom.storage.s3.secret-key", MINIO::getPassword);
    }

    @Autowired
    S3Client s3Client;

    @Autowired
    @Qualifier(S3UploadingClient.STREAMING_CLIENT)
    S3Client s3StreamingClient;

    @Autowired
    S3Presigner s3Presigner;

    @Autowired
    S3Properties s3Properties;

    @Autowired 
    UploadProperties uploadProperties;

    @BeforeEach
    void setUpBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(s3Properties.bucket()).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(s3Properties.bucket()).build());
        }
    }

    @Test
    void contextStartsWithDistinctS3ClientsAndPresigner() {
        assertThat(s3Client).isNotNull();
        assertThat(s3StreamingClient).isNotNull();
        assertThat(s3StreamingClient).isNotSameAs(s3Client);
        assertThat(s3Presigner).isNotNull();
    }

    @Test
    void internalEndpointReachesS3() {
        assertThat(s3Client.listBuckets().buckets())
                .anySatisfy(bucket -> assertThat(bucket.name()).isEqualTo(s3Properties.bucket()));
    }

    @Test
    void presignedUrlIsSignedWithPublicEndpointNotInternal() {
        String key = "sessions/00000000-0000-0000-0000-000000000000/source";

        Duration signatureDuration = uploadProperties.presignTtl();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                PutObjectPresignRequest
                        .builder()
                        .signatureDuration(signatureDuration)
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(s3Properties.bucket())
                                .key(key)
                                .build())
                        .build());

        String url = presigned.url().toString();

        // Presigned URL has public endpoint
        assertThat(url).startsWith(s3Properties.publicEndpoint() + "/" + s3Properties.bucket() + "/" + key);

        assertThat(url).doesNotContain(java.net.URI.create(MINIO.getS3URL()).getAuthority());

        // Presigned URL has signature with correct expiration time
        assertThat(url).contains("X-Amz-Signature").contains(String.format("X-Amz-Expires=%d", signatureDuration.getSeconds()));
    }
}
