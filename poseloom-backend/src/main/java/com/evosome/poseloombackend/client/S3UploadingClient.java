package com.evosome.poseloombackend.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.evosome.poseloombackend.config.S3Properties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * Два "представления" одного хранилища (KB T1.3 п.3):
 * S3Client/S3StreamingS3Client ходят по internal-endpoint (сеть docker),
 * S3Presigner подписывает URL публичным public-endpoint — их нельзя путать,
 * иначе браузер получит ссылку на неименуемый хост (T2.4: "presigned URL
 * подписан
 * публичным хостом, а не minio:9000").
 */
@Configuration(proxyBeanMethods = false)
public class S3UploadingClient {

    public static final String STREAMING_CLIENT = "s3StreamingClient";

    @Autowired
    private S3Properties props;

    /**
     * Default S3 client is used for meta operations such as:
     * - CreateMultipartUpload,
     * - ListParts,
     * - Complete,
     * - Head,
     * - Abort,
     * - Delete.
     */
    @Bean
    public S3Client s3Client() {
        return baseClientBuilder()
                .httpClient(apacheHttpClient(props.timeouts().socket()))
                .overrideConfiguration(
                        ClientOverrideConfiguration
                                .builder()
                                .apiCallTimeout(props.timeouts().apiCall())
                                .apiCallAttemptTimeout(
                                        props.timeouts().apiCallAttempt())
                                .retryStrategy(retryStrategy())
                                .build())
                .build();
    }

    /**
     * Streaming S3 client is used for `GetObject` operation
     * @return S3Client object
     */
    @Bean(name = STREAMING_CLIENT)
    @Qualifier(STREAMING_CLIENT)
    public S3Client s3StreamingClient() {
        return baseClientBuilder()
                .httpClient(apacheHttpClient(props.timeouts().streamingSocket()))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryStrategy(retryStrategy())
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentialsProvider())
                .endpointOverride(URI.create(props.publicEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private S3ClientBuilder baseClientBuilder() {
        return S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentialsProvider())
                .endpointOverride(URI.create(props.internalEndpoint()))
                .forcePathStyle(true);
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
    }

    private SdkHttpClient apacheHttpClient(Duration socketTimeout) {
        return Apache5HttpClient.builder()
                .connectionTimeout(props.timeouts().connect())
                .socketTimeout(socketTimeout)
                .connectionAcquisitionTimeout(props.timeouts().connectionAcquisition())
                .maxConnections(props.maxConnections())
                .build();
    }

    private RetryStrategy retryStrategy() {
        return StandardRetryStrategy.builder()
                .maxAttempts(props.retry().maxAttempts())
                .backoffStrategy(BackoffStrategy.exponentialDelayHalfJitter(
                        props.retry().initialBackoff(), props.retry().maxBackoff()))
                .build();
    }
}
