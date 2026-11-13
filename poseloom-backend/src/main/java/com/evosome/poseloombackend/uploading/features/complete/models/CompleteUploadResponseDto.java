package com.evosome.poseloombackend.uploading.features.complete.models;

/**
 * Обёртка ответа успешного завершения по
 * {@code components/schemas/CompleteUploadResponse.yml}: наружу отдаётся
 * только созданная {@link VideoDto} — все проверки сервер уже выполнил.
 */
public record CompleteUploadResponseDto(VideoDto video) {
}
