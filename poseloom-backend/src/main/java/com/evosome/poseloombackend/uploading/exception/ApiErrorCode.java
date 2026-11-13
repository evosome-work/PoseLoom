package com.evosome.poseloombackend.uploading.exception;

import org.springframework.http.HttpStatus;

/**
 * Машиночитаемые коды ошибок uploading-API — закрытый список из
 * {@code specification/api/v1/components/schemas/ErrorResponse.yml}.
 * Каждый код жёстко связан с HTTP-статусом из {@code components/responses} спеки:
 * 401 — заглушка пользователя, 404 — сессия не найдена или чужая,
 * 409 — состояние/части, 422 — валидации и целостность, 502 — хранилище.
 */
public enum ApiErrorCode {

    /** {@code X-User-Id} отсутствует или невалиден (user stub, T0.1). */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),

    /** Структурно невалидный запрос: пропущено поле, битый JSON/путь. */
    INVALID_REQUEST(HttpStatus.UNPROCESSABLE_CONTENT),

    /** {@code totalBytes > 100 МиБ}. */
    FILE_TOO_LARGE(HttpStatus.UNPROCESSABLE_CONTENT),

    /** {@code totalBytes <= 0}. */
    INVALID_FILE_SIZE(HttpStatus.UNPROCESSABLE_CONTENT),

    /** Расширение {@code originalFilename} не mp4/mov. */
    UNSUPPORTED_FORMAT(HttpStatus.UNPROCESSABLE_CONTENT),

    /** {@code checksumSha256} — не 64 hex-символа в нижнем регистре. */
    INVALID_CHECKSUM_FORMAT(HttpStatus.UNPROCESSABLE_CONTENT),

    /** Нет сессии или она чужая: существование чужой не раскрывается. */
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** {@code partNumber} вне диапазона {@code 1..partCount}. */
    PART_NUMBER_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_CONTENT),

    /** Операция в терминальном статусе сессии (presign в COMPLETED/FAILED). */
    INVALID_SESSION_STATE(HttpStatus.CONFLICT),

    /** Complete уже отказнувшей сессии. */
    SESSION_FAILED(HttpStatus.CONFLICT),

    /** Retryable: не хватает частей, сессия остаётся CREATED. */
    MISSING_PARTS(HttpStatus.CONFLICT),

    /** Retryable: размер части или сумма не совпадают, сессия остаётся CREATED. */
    PART_SIZE_MISMATCH(HttpStatus.CONFLICT),

    /** Terminal: {@code HeadObject.ContentLength != totalBytes}. */
    SIZE_MISMATCH(HttpStatus.CONFLICT),

    /** Terminal: пересчитанный SHA-256 объекта не совпал с заявленным. */
    CHECKSUM_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT),

    /** Terminal: нет {@code ftyp} или major brand вне whitelist. */
    UNSUPPORTED_CONTAINER(HttpStatus.UNPROCESSABLE_CONTENT),

    /** Transient: ошибка объектного хранилища; сессия не разрушается. */
    STORAGE_ERROR(HttpStatus.BAD_GATEWAY);

    private final HttpStatus httpStatus;

    ApiErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
