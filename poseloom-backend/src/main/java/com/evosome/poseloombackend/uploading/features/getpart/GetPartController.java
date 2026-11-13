package com.evosome.poseloombackend.uploading.features.getpart;

import com.evosome.poseloombackend.uploading.common.CurrentUserResolver;
import com.evosome.poseloombackend.uploading.features.getpart.models.PartUploadUrlDto;
import com.evosome.poseloombackend.uploading.features.getpart.services.PresignPartService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * {@code POST /api/v1/upload-sessions/{sessionId}/parts/{partNumber}/url} —
 * presigned URL части (спека uploading/getpart.yml, operationId
 * {@code getPresignedPartUrl}). Тело запроса отсутствует; успех — {@code 200}
 * с {@link PartUploadUrlDto}; ошибки — 401/404/409/422/502 по спеке.
 */
@RestController
@RequestMapping("/api/v1/upload-sessions")
@RequiredArgsConstructor
public class GetPartController {

    private final PresignPartService presignPartService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/{sessionId}/parts/{partNumber}/url")
    public ResponseEntity<PartUploadUrlDto> getPresignedPartUrl(
            @RequestHeader(value = "X-User-Id", required = true) String userId,
            @PathVariable UUID sessionId,
            @PathVariable int partNumber) {

        return ResponseEntity.ok(presignPartService.presign(
                sessionId, currentUserResolver.resolve(userId), partNumber));
    }
}
