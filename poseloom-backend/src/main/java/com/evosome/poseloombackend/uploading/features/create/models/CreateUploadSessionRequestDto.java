package com.evosome.poseloombackend.uploading.features.create.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Заявка на multipart-сессию по {@code components/schemas/CreateUploadSessionRequest.yml}.
 * Структурные проверки (null/длина имени) — Bean Validation → 422 INVALID_REQUEST;
 * содержательные (размер, расширение, формат checksum) — в сервисе с точными
 * кодами спеки: {@code INVALID_FILE_SIZE}, {@code FILE_TOO_LARGE},
 * {@code UNSUPPORTED_FORMAT}, {@code INVALID_CHECKSUM_FORMAT}.
 *
 * @param originalFilename исходное имя файла — только для отображения,
 *                         ключ объекта от него не зависит
 * @param totalBytes       точный размер файла в байтах, {@code (0; 100 MiB]}
 * @param checksumSha256   SHA-256 всего файла: 64 hex-символа в нижнем регистре
 */
public record CreateUploadSessionRequestDto(

        @NotNull
        @Size(min = 1, max = 255)
        String originalFilename,

        @NotNull
        Long totalBytes,

        @NotNull
        String checksumSha256) {
}
