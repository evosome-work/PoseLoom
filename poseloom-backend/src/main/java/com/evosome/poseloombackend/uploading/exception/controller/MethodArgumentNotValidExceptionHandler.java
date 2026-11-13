package com.evosome.poseloombackend.uploading.exception.controller;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiErrorMessage;
import com.evosome.poseloombackend.uploading.exception.model.ApiErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice провалидированных DTO ({@code @Valid} не прошёл) →
 * {@code 422 INVALID_REQUEST}. Структурные поля спеки (required/размер имени)
 * проверяются Bean Validation; содержательные коды ({@code FILE_TOO_LARGE} и
 * т.п.) генерирует сервис, а не этот advice.
 */
@RestControllerAdvice
public class MethodArgumentNotValidExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(MethodArgumentNotValidExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidRequest(MethodArgumentNotValidException e) {
        log.info("Validation failed: {}", e.getAllErrors());
        return ApiErrorResponses.of(ApiErrorCode.INVALID_REQUEST, ApiErrorMessage.INVALID_REQUEST_MESSAGE);
    }
}
