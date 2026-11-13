package com.evosome.poseloombackend.uploading.exception.controller;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiErrorMessage;
import com.evosome.poseloombackend.uploading.exception.model.ApiErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice непарсящегося тела запроса (битый JSON, неверный тип поля) →
 * {@code 422 INVALID_REQUEST} без деталей парсера Jackson.
 */
@RestControllerAdvice
public class HttpMessageNotReadableExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(HttpMessageNotReadableExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.info("Malformed request body: {}", e.getMessage());
        return ApiErrorResponses.of(ApiErrorCode.INVALID_REQUEST, ApiErrorMessage.INVALID_REQUEST_MESSAGE);
    }
}
