package com.evosome.poseloombackend.uploading.exception.controller;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiErrorMessage;
import com.evosome.poseloombackend.uploading.exception.StorageException;
import com.evosome.poseloombackend.uploading.exception.model.ApiErrorResponseDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice {@link StorageException} → {@code 502 STORAGE_ERROR}
 * (спека StorageError.yml): transient, сессия не разрушается, клиент может
 * повторить операцию. Детали SDK наружу не отдаются.
 */
@RestControllerAdvice
public class StorageExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(StorageExceptionHandler.class);

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponseDto> handleStorage(StorageException e) {
        log.warn("Storage failure: {}", e.getMessage());
        return ApiErrorResponses.of(ApiErrorCode.STORAGE_ERROR, ApiErrorMessage.STORAGE_UNAVAILABLE_MESSAGE);
    }
}
