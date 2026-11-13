package com.evosome.poseloombackend.uploading.feature.create.models;

import java.util.UUID;

/**
 * Ответ {@code 201} на создание сессии по
 * {@code components/schemas/UploadSessionCreated.yml}: {@code CreateMultipartUpload}
 * уже выполнен, запись сохранена в статусе {@code CREATED}.
 *
 * @param sessionId идентификатор сессии загрузки
 * @param partSize  фиксированный размер части, байт (10 MiB)
 * @param partCount {@code ceil(totalBytes / partSize)}, не более 10
 */
public record UploadSessionCreatedDto(UUID sessionId, long partSize, int partCount) {
}
