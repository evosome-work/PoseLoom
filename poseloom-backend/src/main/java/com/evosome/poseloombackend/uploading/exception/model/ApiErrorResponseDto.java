package com.evosome.poseloombackend.uploading.exception.model;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;

/**
 * Единый формат ошибки по {@code components/schemas/ErrorResponse.yml}:
 * только {@code code} и {@code message}, без стектрейсов и внутренних путей.
 *
 * @param code   машиночитаемый код — имя {@link ApiErrorCode}
 * @param message человекочитаемое описание
 */
public record ApiErrorResponseDto(String code, String message) {

    public static ApiErrorResponseDto of(ApiErrorCode code, String message) {
        return new ApiErrorResponseDto(code.name(), message);
    }
}
