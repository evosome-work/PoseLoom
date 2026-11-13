package com.evosome.poseloombackend.uploading.exception.controller;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiErrorMessage;
import com.evosome.poseloombackend.uploading.exception.model.ApiErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Advice path/query-параметра неверного типа (не-UUID {@code sessionId},
 * не-число {@code partNumber}) → {@code 422 INVALID_REQUEST}.
 */
@RestControllerAdvice
public class MethodArgumentTypeMismatchExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(MethodArgumentTypeMismatchExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.info("Malformed request parameter: {}", e.getMessage());
        return ApiErrorResponses.of(ApiErrorCode.INVALID_REQUEST, ApiErrorMessage.INVALID_REQUEST_MESSAGE);
    }
}
