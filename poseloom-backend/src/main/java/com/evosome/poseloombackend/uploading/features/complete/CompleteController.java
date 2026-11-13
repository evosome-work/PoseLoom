package com.evosome.poseloombackend.uploading.features.complete;

import com.evosome.poseloombackend.uploading.common.CurrentUserResolver;
import com.evosome.poseloombackend.uploading.features.complete.models.CompleteUploadResponseDto;
import com.evosome.poseloombackend.uploading.features.complete.services.CompleteUploadResult;
import com.evosome.poseloombackend.uploading.features.complete.services.CompleteUploadService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * {@code POST /api/v1/upload-sessions/{sessionId}/complete} — проверить части,
 * собрать объект и создать Video (спека uploading/complete.yml, operationId
 * {@code completeUploadSession}). Тело запроса пустое. Успех первого complete —
 * {@code 201}, повторного — {@code 200} с той же Video; ошибки —
 * 401/404/409/422/502 по спеке через {@code UploadExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/upload-sessions")
@RequiredArgsConstructor
public class CompleteController {

    private final CompleteUploadService completeUploadService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<CompleteUploadResponseDto> completeUploadSession(
            @RequestHeader(value = "X-User-Id", required = true) String userId,
            @PathVariable UUID sessionId) {

        CompleteUploadResult result =
                completeUploadService.complete(sessionId, currentUserResolver.resolve(userId));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(new CompleteUploadResponseDto(result.video()));
    }
}
