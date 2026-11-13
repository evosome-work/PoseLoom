package com.evosome.poseloombackend.uploading.exception;

import lombok.Getter;

/**
 * Ошибка загрузки видео файла с машиночитаемым кодом из {@link ApiErrorCode}.
 * Сообщение — человекочитаемое, без стектрейсов и внутренних путей хранилища
 * (ErrorResponse.yml). Маппится в HTTP-ответ через {@link UploadExceptionHandler}.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ApiErrorCode code;

    public ApiException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(ApiErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
