package com.evosome.poseloombackend.uploading.features.create;

import com.evosome.poseloombackend.uploading.common.CurrentUserResolver;
import com.evosome.poseloombackend.uploading.features.create.models.CreateUploadSessionRequestDto;
import com.evosome.poseloombackend.uploading.features.create.models.UploadSessionCreatedDto;
import com.evosome.poseloombackend.uploading.features.create.services.CreateUploadSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/v1/upload-sessions} — создать сессию и multipart-upload
 * (спека uploading/create.yml, operationId {@code createUploadSession}).
 * Успех — {@code 201} с {@link UploadSessionCreatedDto}; ошибки маппит
 * {@code UploadExceptionHandler} (401/422/502).
 */
@RestController
@RequestMapping("/api/v1/upload-sessions")
@RequiredArgsConstructor
public class CreateController {

    private final CreateUploadSessionService createUploadSessionService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<UploadSessionCreatedDto> createUploadSession(
            @RequestHeader(value = "X-User-Id", required = true) String userId,
            @Valid @RequestBody CreateUploadSessionRequestDto request) {

        UploadSessionCreatedDto response =
                createUploadSessionService.create(currentUserResolver.resolve(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
