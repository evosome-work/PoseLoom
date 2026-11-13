package com.evosome.poseloombackend.uploading.features.getpart.services;

import com.evosome.poseloombackend.config.UploadProperties;
import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiException;
import com.evosome.poseloombackend.uploading.features.getpart.models.PartUploadUrlDto;
import com.evosome.poseloombackend.uploading.model.SessionStatus;
import com.evosome.poseloombackend.uploading.model.UploadSession;
import com.evosome.poseloombackend.uploading.repository.UploadSessionRepository;
import com.evosome.poseloombackend.uploading.storage.ObjectStorageGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Выдача presigned {@code UploadPart} URL части (Tasks T3.2, спека getpart.yml).
 * Порядок проверок по спеке: владелец сессии (чужая → тот же 404) → состояние
 * {@code CREATED} (терминальное → 409 {@code INVALID_SESSION_STATE}) →
 * {@code 1 <= partNumber <= partCount} (иначе 422). Подпись — публичным
 * endpoint на TTL из {@link UploadProperties}; истёкший URL отклоняет
 * хранилище, клиент запрашивает новый.
 */
@Service
@RequiredArgsConstructor
public class PresignPartService {

    private final UploadSessionRepository uploadSessionRepository;
    private final UploadProperties uploadProperties;
    private final ObjectStorageGateway storageGateway;

    public PartUploadUrlDto presign(UUID sessionId, UUID uploaderId, int partNumber) {
        UploadSession session = uploadSessionRepository
                .findByIdAndUploaderId(sessionId, uploaderId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.SESSION_NOT_FOUND,
                        "Upload session not found"));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new ApiException(ApiErrorCode.INVALID_SESSION_STATE,
                    "Unable to resolve presigned URL to load file, "
                            + "because uploading session is already completed");
        }
        
        if (session.getStatus() == SessionStatus.FAILED) {
            throw new ApiException(ApiErrorCode.INVALID_SESSION_STATE,
                    "Unable to resolve presigned URL to load file, "
                            + "because uploading session is failed");
        }

        if (partNumber < 1 || partNumber > session.getPartCount()) {
            throw new ApiException(ApiErrorCode.PART_NUMBER_OUT_OF_RANGE,
                    "partNumber %d is outside 1..%d".formatted(partNumber, session.getPartCount()));
        }

        ObjectStorageGateway.PresignedPartUrl presigned = storageGateway.presignUploadPart(
                session.getBucket(),
                session.getStoragePath(),
                session.getUploadId(),
                partNumber,
                uploadProperties.presignTtl());

        return new PartUploadUrlDto(presigned.url(), partNumber, presigned.expiresAt());
    }
}
