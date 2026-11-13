package com.evosome.poseloombackend.uploading.exception.controller;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.model.ApiErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice нарушения constraints при complete: unique
 * {@code videos.upload_session_id} — финальная защита от дубля параллельных
 * complete (Tasks T3.5); при нехватке lock-защиты отдаём
 * {@code 409 INVALID_SESSION_STATE} («Session state changed concurrently»),
 * а не 500 с текстом драйвера. Прочие integrity-нарушения — баг кода
 * (ER Deviations), в тот же 409 по контракту наружу.
 */
@RestControllerAdvice
public class DataIntegrityViolationExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(DataIntegrityViolationExceptionHandler.class);

    private static final String CONCURRENT_STATE = "Session state changed concurrently";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleRace(DataIntegrityViolationException e) {
        log.warn("Integrity violation, session state changed concurrently", e);
        return ApiErrorResponses.of(ApiErrorCode.INVALID_SESSION_STATE, CONCURRENT_STATE);
    }
}
