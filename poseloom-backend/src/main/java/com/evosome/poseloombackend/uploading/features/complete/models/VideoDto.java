package com.evosome.poseloombackend.uploading.features.complete.models;

import com.evosome.poseloombackend.uploading.model.Video;

import java.time.Instant;
import java.util.UUID;

/**
 * Сущность {@link Video} в формате {@code components/schemas/Video.yml}:
 * объект собран, размеры и SHA-256 сверены, {@code ftyp} распознан.
 * {@code format} — container по сигнатуре; кодек, длительность и разрешение —
 * эпик 002 (в схеме ответа их нет).
 *
 * @param ownerId владелец; в эпике 001 — из заглушки X-User-Id
 * @param format  {@code MP4} или {@code MOV}
 */
public record VideoDto(UUID id,
                       UUID ownerId,
                       String originalFilename,
                       String storagePath,
                       long sizeBytes,
                       String checksumSha256,
                       String format,
                       Instant uploadedAt) {

    public static VideoDto from(Video video) {
        return new VideoDto(
                video.getId(),
                video.getOwnerId(),
                video.getOriginalFilename(),
                video.getStoragePath(),
                video.getSizeBytes(),
                video.getChecksumSha256(),
                video.getContainer().name(),
                video.getUploadedAt());
    }
}
