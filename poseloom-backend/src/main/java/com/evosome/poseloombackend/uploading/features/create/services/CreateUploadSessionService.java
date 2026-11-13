package com.evosome.poseloombackend.uploading.features.create.services;

import com.evosome.poseloombackend.config.S3Properties;
import com.evosome.poseloombackend.config.UploadProperties;
import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiException;
import com.evosome.poseloombackend.uploading.features.create.models.CreateUploadSessionRequestDto;
import com.evosome.poseloombackend.uploading.features.create.models.UploadSessionCreatedDto;
import com.evosome.poseloombackend.uploading.model.FailureCode;
import com.evosome.poseloombackend.uploading.model.SessionStatus;
import com.evosome.poseloombackend.uploading.model.UploadSession;
import com.evosome.poseloombackend.uploading.repository.UploadSessionRepository;
import com.evosome.poseloombackend.uploading.storage.ObjectStorageGateway;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Создание multipart-сессии (Tasks T3.1, спека create.yml). Порядок сервера до
 * ответа: валидация заявки → {@code CreateMultipartUpload} → {@code INSERT
 * upload_sessions} в {@code CREATED}. Сбой хранилища — сессия не создаётся,
 * {@code STORAGE_ERROR} (502) уходит из gateway как transient. Сбой вставки —
 * компенсация {@code AbortMultipartUpload}, чтобы uploadId не осиротел.
 * Ключ объекта {@code sessions/{sessionId}/source} не зависит от имени файла;
 * sessionId присваивается здесь до вставки (назначаемый id сущности).
 */
@Service
@RequiredArgsConstructor
public class CreateUploadSessionService {

    /** Канон T0.1: ровно 64 hex-символа в нижнем регистре. */
    private static final Pattern CHECKSUM_SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private static final Logger log = LoggerFactory.getLogger(CreateUploadSessionService.class);

    private final UploadProperties uploadProperties;
    private final S3Properties s3Properties;
    private final ObjectStorageGateway storageGateway;
    private final UploadSessionRepository uploadSessionRepository;

    public UploadSessionCreatedDto create(UUID uploaderId, CreateUploadSessionRequestDto request) {
        long totalBytes = request.totalBytes();
        long partCount = validate(request);

        UUID sessionId = UUID.randomUUID();
        String storagePath = uploadProperties.objectKeyTemplate()
                .replace("{sessionId}", sessionId.toString());

        String uploadId = storageGateway.createMultipartUpload(s3Properties.bucket(), storagePath);

        Instant now = Instant.now();
        UploadSession session = new UploadSession();
        session.setId(sessionId);
        session.setUploaderId(uploaderId);
        session.setStatus(SessionStatus.CREATED);
        session.setBucket(s3Properties.bucket());
        session.setStoragePath(storagePath);
        session.setUploadId(uploadId);
        session.setOriginalFilename(request.originalFilename());
        session.setTotalBytes(totalBytes);
        session.setPartSize((int) uploadProperties.partSize());
        session.setPartCount((int) partCount);
        session.setChecksumSha256(request.checksumSha256());
        session.setFailureCode(FailureCode.NONE);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        try {
            uploadSessionRepository.save(session);
        } catch (RuntimeException persistenceFailure) {
            abortQuietly(storagePath, uploadId);
            throw persistenceFailure;
        }

        log.debug("Upload session {} created for uploader {}", sessionId, uploaderId);
        return new UploadSessionCreatedDto(sessionId, uploadProperties.partSize(), (int) partCount);
    }

    /**
     * Содержательные проверки заявки с точными кодами спеки; порядок — как в
     * Upload Flow: размер → расширение → checksum → partCount.
     *
     * @return {@code partCount = ceil(totalBytes / partSize)}
     */
    private long validate(CreateUploadSessionRequestDto request) {
        long totalBytes = request.totalBytes();

        if (totalBytes <= 0) {
            throw new ApiException(ApiErrorCode.INVALID_FILE_SIZE, "totalBytes must be greater than 0");
        }
        if (totalBytes > uploadProperties.maxTotalBytes()) {
            throw new ApiException(ApiErrorCode.FILE_TOO_LARGE,
                    "totalBytes %d exceeds the %d MiB limit".formatted(
                            totalBytes, uploadProperties.maxTotalBytes() / (1024 * 1024)));
        }

        if (!isExtensionAllowed(request.originalFilename())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_FORMAT,
                    "Only %s files are accepted".formatted(
                            uploadProperties.allowedExtensions().stream()
                                    .sorted()
                                    .map(ext -> "." + ext)
                                    .reduce((a, b) -> a + " and " + b)
                                    .orElse(".mp4 and .mov")));
        }

        if (!CHECKSUM_SHA256.matcher(request.checksumSha256()).matches()) {
            throw new ApiException(ApiErrorCode.INVALID_CHECKSUM_FORMAT,
                    "checksumSha256 must be 64 hex characters");
        }

        long partCount = uploadProperties.partCount(totalBytes);
        if (partCount > uploadProperties.maxParts()) {
            throw new ApiException(ApiErrorCode.FILE_TOO_LARGE,
                    "totalBytes %d requires %d parts, more than maxParts %d"
                            .formatted(totalBytes, partCount, uploadProperties.maxParts()));
        }
        return partCount;
    }

    private boolean isExtensionAllowed(String originalFilename) {
        String lowercased = originalFilename.toLowerCase(Locale.ROOT);
        int dot = lowercased.lastIndexOf('.');
        return dot >= 0 && uploadProperties.allowedExtensions()
                .contains(lowercased.substring(dot + 1));
    }

    /** Компенсация T3.1: сбой вставки не должен оставлять сиротский multipart-upload. */
    private void abortQuietly(String storagePath, String uploadId) {
        try {
            storageGateway.abortMultipartUpload(s3Properties.bucket(), storagePath, uploadId);
        } catch (RuntimeException cleanupFailure) {
            log.warn("Compensating abort failed for key={} uploadId={}: {}",
                    storagePath, uploadId, cleanupFailure.toString());
        }
    }
}
