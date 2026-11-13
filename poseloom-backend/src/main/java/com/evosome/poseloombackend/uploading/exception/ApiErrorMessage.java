package com.evosome.poseloombackend.uploading.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiErrorMessage {
    public static final String INVALID_REQUEST_MESSAGE = "Invalid request body or parameters";
    public static final String STORAGE_UNAVAILABLE_MESSAGE = "Object storage is temporarily unavailable";
}
