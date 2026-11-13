package com.evosome.poseloombackend.uploading.exception.controller;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.model.ApiErrorResponseDto;
import org.springframework.http.ResponseEntity;

/**
 * Общий билдер HTTP-ответа ошибки для advice-классов пакета: статус берётся из
 * {@link ApiErrorCode#httpStatus()}, тело — {@link ApiErrorResponseDto} без
 * стектрейсов и внутренних путей (ErrorResponse.yml).
 */
final class ApiErrorResponses {

    private ApiErrorResponses() {
    }

    static ResponseEntity<ApiErrorResponseDto> of(ApiErrorCode code, String message) {
        return ResponseEntity.status(code.httpStatus())
                .body(ApiErrorResponseDto.of(code, message));
    }
}
