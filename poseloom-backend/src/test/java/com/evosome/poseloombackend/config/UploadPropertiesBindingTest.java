package com.evosome.poseloombackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class UploadPropertiesBindingTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({UploadProperties.class, S3Properties.class})
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
                    .of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(Config.class);

    private String[] validProperties() {
        return new String[]{
                "poseloom.upload.max-total-bytes=104857600",
                "poseloom.upload.part-size=10485760",
                "poseloom.upload.max-parts=10",
                "poseloom.upload.presign-ttl=PT15M",
                "poseloom.upload.allowed-extensions[0]=MP4",
                "poseloom.upload.allowed-extensions[1]=mov",
                "poseloom.upload.object-key-template=sessions/{sessionId}/source",
                "poseloom.storage.s3.bucket=videos",
                "poseloom.storage.s3.max-connections=50",
                "poseloom.storage.s3.region=us-east-1",
                "poseloom.storage.s3.internal-endpoint=http://internal:9000",
                "poseloom.storage.s3.public-endpoint=http://public:9000",
                "poseloom.storage.s3.access-key=ak",
                "poseloom.storage.s3.secret-key=sk",
                "poseloom.storage.s3.timeouts.api-call=PT30S",
                "poseloom.storage.s3.timeouts.api-call-attempt=PT10S",
                "poseloom.storage.s3.timeouts.connect=PT5S",
                "poseloom.storage.s3.timeouts.socket=PT60S",
                "poseloom.storage.s3.timeouts.streaming-socket=PT2M",
                "poseloom.storage.s3.timeouts.connection-acquisition=PT5S",
                "poseloom.storage.s3.retry.max-attempts=3",
                "poseloom.storage.s3.retry.initial-backoff=PT0.5S",
                "poseloom.storage.s3.retry.max-backoff=PT20S",
        };
    }

    @Test
    void bindsValidConfiguration() {
        runner.withPropertyValues(validProperties()).run(context -> {
            UploadProperties upload = context.getBean(UploadProperties.class);
            assertThat(upload.presignTtl()).isEqualTo(Duration.ofMinutes(15));
            assertThat(upload.maxTotalBytes()).isEqualTo(104_857_600L);
            assertThat(upload.allowedExtensions()).containsExactlyInAnyOrder("mp4", "mov");
            assertThat(upload.partCount(73_400_320L)).isEqualTo(7);

            S3Properties s3 = context.getBean(S3Properties.class);
            assertThat(s3.bucket()).isEqualTo("videos");
            assertThat(s3.retry().maxAttempts()).isEqualTo(3);
        });
    }

    @Test
    void rejectsNonPositivePartSize() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("poseloom.upload.part-size=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsInconsistentLimits() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("poseloom.upload.max-total-bytes=209715200")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsObjectKeyTemplateWithoutSessionId() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("poseloom.upload.object-key-template=uploads/source")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsBlankPublicEndpoint() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("poseloom.storage.s3.public-endpoint=")
                .run(context -> assertThat(context).hasFailed());
    }
}
