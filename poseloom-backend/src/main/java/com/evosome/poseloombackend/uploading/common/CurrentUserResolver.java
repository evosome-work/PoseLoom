package com.evosome.poseloombackend.uploading.common;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Заглушка текущей аутентификации эпика 001 (решение T0.1): пользователь берётся
 * из заголовка {@code X-User-Id}. Отсутствующий или непарсящийся заголовок —
 * 401 {@code UNAUTHORIZED}; таблица {@code users} в эпике отсутствует, FK нет.
 */
@Component
public class CurrentUserResolver {

    private static final String UNAUTHORIZED_MESSAGE = "Missing or malformed X-User-Id header";

    /**
     * Разбирает значение заголовка {@code X-User-Id}.
     *
     * @param headerValue сырое значение заголовка или {@code null}, если заголовок не прислан
     * @return идентификатор текущего пользователя
     * @throws ApiException {@code UNAUTHORIZED} при отсутствии/битом заголовке
     */
    public UUID resolve(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }
        try {
            return UUID.fromString(headerValue.trim());
        } catch (IllegalArgumentException malformed) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }
    }
}
