package com.evosome.poseloombackend.uploading.exception.controller;

import com.evosome.poseloombackend.uploading.exception.ApiException;
import com.evosome.poseloombackend.uploading.exception.model.ApiErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice доменной {@link ApiException}: код и HTTP-статус берутся из
 * {@code e.getCode()} (маппинг спеки components/responses), сообщение — из
 * исключения. Сообщения бизнес-ошибок не содержат внутренних путей хранилища.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponseDto> handleApiException(ApiException e) {
        log.info("Rejected request: {} — {}", e.getCode(), e.getMessage());
        return ApiErrorResponses.of(e.getCode(), e.getMessage());
    }
}
