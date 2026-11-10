package com.evosome.poseloombackend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Подключение к S3-совместимому хранилищу (Silo/MinIO).
 * internal-endpoint — адрес, которым оркестратор сам ходит в хранилище;
 * public-endpoint — адрес, которым подписываются presigned URL для клиента
 */
@ConfigurationProperties(prefix = "poseloom.storage.s3")
@Validated
public record S3Properties(
        @NotBlank String bucket,
        @NotBlank String region,
        @NotBlank String internalEndpoint,
        @NotBlank String publicEndpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @Positive int maxConnections,
        @NotNull @Valid Timeouts timeouts,
        @NotNull @Valid Retry retry) {

    /**
     * Таймауты AWS SDK v2:
     * apiCall — весь вызов с учётом ретраев (только для metadata-операций),
     * apiCallAttempt — одна попытка,
     * streamingSocket — сокет-таймаут отдельного клиента для потокового GET
     * (SHA-256 до 100 MiB),
     * чтобы общий apiCallTimeout не обрывал легитимное чтение тела.
     */
    public record Timeouts(
            @NotNull Duration apiCall,
            @NotNull Duration apiCallAttempt,
            @NotNull Duration connect,
            @NotNull Duration socket,
            @NotNull Duration streamingSocket,
            @NotNull Duration connectionAcquisition) {
    }

    public record Retry(
            @Min(1) int maxAttempts,
            @NotNull Duration initialBackoff,
            @NotNull Duration maxBackoff) {
    }
}
