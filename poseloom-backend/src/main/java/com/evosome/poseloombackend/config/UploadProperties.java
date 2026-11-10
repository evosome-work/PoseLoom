package com.evosome.poseloombackend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Лимиты загрузки по стеку Epic 001 (KB "Upload Flow", таблица "Параметры загрузки"):
 * max 100 MiB, часть 10 MiB, до 10 частей, TTL presigned URL 15 минут, mp4/mov,
 * ключ объекта sessions/{sessionId}/source.
 */
@ConfigurationProperties(prefix = "poseloom.upload")
@Validated
public record UploadProperties(
        @Positive long maxTotalBytes,
        @Positive long partSize,
        @Positive int maxParts,
        @NotNull Duration presignTtl,
        @NotEmpty Set<String> allowedExtensions,
        @NotBlank String objectKeyTemplate) {

    public UploadProperties {
        allowedExtensions = allowedExtensions.stream()
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        long requiredParts = ceilParts(maxTotalBytes, partSize);
        if (requiredParts > maxParts) {
            throw new IllegalStateException(
                    "Inconsistent upload limits: ceil(maxTotalBytes/partSize)=%d exceeds maxParts=%d"
                            .formatted(requiredParts, maxParts));
        }
        if (!objectKeyTemplate.contains("{sessionId}")) {
            throw new IllegalStateException(
                    "objectKeyTemplate must contain {sessionId} placeholder: " + objectKeyTemplate);
        }
    }

    public long partCount(long totalBytes) {
        return ceilParts(totalBytes, partSize);
    }

    private static long ceilParts(long totalBytes, long partSize) {
        return Math.floorDiv(totalBytes, partSize) + (totalBytes % partSize == 0 ? 0 : 1);
    }
}
