package com.evosome.poseloombackend.uploading.feature.complete.services;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiException;
import com.evosome.poseloombackend.uploading.feature.complete.models.VideoDto;
import com.evosome.poseloombackend.uploading.model.FailureCode;
import com.evosome.poseloombackend.uploading.model.SessionStatus;
import com.evosome.poseloombackend.uploading.model.UploadSession;
import com.evosome.poseloombackend.uploading.model.Video;
import com.evosome.poseloombackend.uploading.model.VideoContainer;
import com.evosome.poseloombackend.uploading.repository.UploadSessionRepository;
import com.evosome.poseloombackend.uploading.repository.VideoRepository;
import com.evosome.poseloombackend.uploading.storage.ObjectStorageGateway;
import com.evosome.poseloombackend.uploading.storage.ObjectStorageGateway.StoredPart;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Завершение загрузки (Tasks T3.3/T3.4/T3.5, спека complete.yml). Порядок:
 * чтение сессии под pessimistic lock (сериализует параллельные complete) →
 * {@code ListParts} все страницы → непрерывность {@code 1..partCount} →
 * размеры частей → сумма → {@code CompleteMultipartUpload} с ETag из ListParts →
 * {@code HeadObject == totalBytes} → {@code ftyp} → потоковый SHA-256 →
 * одна транзакция {@code INSERT videos + UPDATE session COMPLETED}.
 * Retryable (MISSING_PARTS, PART_SIZE_MISMATCH) бросаются до любых записей —
 * сессия остаётся CREATED. Terminal (SIZE_MISMATCH, CHECKSUM_MISMATCH,
 * UNSUPPORTED_CONTAINER) пишут FAILED+failure_code, делают cleanup (delete
 * объекта, abort upload) и пробрасываются после коммита, чтобы запись об отказе
 * не откатилась. Transient StorageExceptionrollback-ит транзакцию без
 * разрушения сессии (502).
 */
@Service
@RequiredArgsConstructor
public class CompleteUploadService {

    private static final Logger log = LoggerFactory.getLogger(CompleteUploadService.class);

    private final UploadSessionRepository uploadSessionRepository;
    private final VideoRepository videoRepository;
    private final ObjectStorageGateway storageGateway;
    private final PlatformTransactionManager transactionManager;

    /**
     * @return {@code video} + {@code created=true} для первого complete (201)
     *         и {@code created=false} для повторного (200, та же Video)
     */
    public CompleteUploadResult complete(UUID sessionId, UUID uploaderId) {
        Attempt attempt = new TransactionTemplate(transactionManager)
                .execute(status -> attemptComplete(sessionId, uploaderId));
        if (attempt == null) {
            throw new IllegalStateException("Transaction callback returned no attempt");
        }
        if (attempt.pendingError() != null) {
            throw attempt.pendingError();
        }
        return new CompleteUploadResult(VideoDto.from(attempt.video()), attempt.created());
    }

    private Attempt attemptComplete(UUID sessionId, UUID uploaderId) {
        UploadSession session = uploadSessionRepository
                .findByIdAndUploaderIdForUpdate(sessionId, uploaderId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.SESSION_NOT_FOUND,
                        "Upload session not found"));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            Video video = videoRepository.findByUploadSessionId(session.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Session " + session.getId() + " is COMPLETED without Video"));
            return new Attempt(video, false, null);
        }
        if (session.getStatus() == SessionStatus.FAILED) {
            throw new ApiException(ApiErrorCode.SESSION_FAILED,
                    "Session has failed and cannot be completed");
        }

        List<StoredPart> parts = storageGateway.listAllParts(
                session.getBucket(), session.getStoragePath(), session.getUploadId());
        verifyParts(session, parts);

        storageGateway.completeMultipartUpload(
                session.getBucket(), session.getStoragePath(), session.getUploadId(), parts);

        long objectSize = storageGateway.headObjectSize(session.getBucket(), session.getStoragePath());
        if (objectSize != session.getTotalBytes()) {
            return failAndCleanup(session, FailureCode.SIZE_MISMATCH, new ApiException(
                    ApiErrorCode.SIZE_MISMATCH,
                    "Object size %d does not match totalBytes %d".formatted(
                            objectSize, session.getTotalBytes())));
        }

        VideoContainer container;
        try {
            byte[] head = storageGateway.rangeGet(session.getBucket(), session.getStoragePath(),
                    0, FtypInspector.HEADER_BYTES - 1L);
            container = FtypInspector.inspect(head);
        } catch (ApiException unsupported) {
            return failAndCleanup(session, FailureCode.UNSUPPORTED_CONTAINER, unsupported);
        }

        String actualSha256;
        try (InputStream stream = storageGateway.openObjectStream(
                session.getBucket(), session.getStoragePath())) {
            actualSha256 = Sha256Verifier.hexDigest(stream);
        } catch (IOException readFailure) {
            throw new com.evosome.poseloombackend.uploading.storage.StorageException(
                    "Failed to stream assembled object", readFailure);
        }
        if (!actualSha256.equals(session.getChecksumSha256())) {
            return failAndCleanup(session, FailureCode.CHECKSUM_MISMATCH, new ApiException(
                    ApiErrorCode.CHECKSUM_MISMATCH,
                    "SHA-256 of the assembled object does not match checksumSha256"));
        }

        Instant now = Instant.now();
        Video video = new Video();
        video.setOwnerId(session.getUploaderId());
        video.setUploadSessionId(session.getId());
        video.setOriginalFilename(session.getOriginalFilename());
        video.setStoragePath(session.getStoragePath());
        video.setSizeBytes(objectSize);
        video.setChecksumSha256(session.getChecksumSha256());
        video.setContainer(container);
        video.setCreatedAt(now);
        video.setUploadedAt(now);

        videoRepository.save(video);
        session.complete(now);
        uploadSessionRepository.save(session);

        log.info("Session {} completed: video created, container={}, size={}",
                session.getId(), container, objectSize);
        return new Attempt(video, true, null);
    }

    /**
     * Проверки набора частей (спека complete.yml, пп. 1–4): непрерывность
     * {@code 1..partCount} → {@code MISSING_PARTS}; каждая часть кроме последней
     * равна {@code partSize}, сумма равна {@code totalBytes} →
     * {@code PART_SIZE_MISMATCH}. Обе — retryable: бросок до записей в БД,
     * сессия остаётся {@code CREATED}, клиент досылает части и повторяет.
     */
    private void verifyParts(UploadSession session, List<StoredPart> parts) {
        Map<Integer, StoredPart> byNumber = new HashMap<>();
        for (StoredPart part : parts) {
            byNumber.put(part.partNumber(), part);
        }

        List<Integer> missing = new ArrayList<>();
        for (int number = 1; number <= session.getPartCount(); number++) {
            if (!byNumber.containsKey(number)) {
                missing.add(number);
            }
        }
        if (!missing.isEmpty()) {
            throw new ApiException(ApiErrorCode.MISSING_PARTS,
                    "Parts %s were not uploaded".formatted(
                            missing.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        }

        for (int number = 1; number < session.getPartCount(); number++) {
            StoredPart part = byNumber.get(number);
            if (part.size() != session.getPartSize()) {
                throw new ApiException(ApiErrorCode.PART_SIZE_MISMATCH,
                        "Part %d size %d does not match partSize %d".formatted(
                                number, part.size(), session.getPartSize()));
            }
        }

        long sum = 0;
        for (StoredPart part : parts) {
            sum += part.size();
        }
        if (sum != session.getTotalBytes()) {
            throw new ApiException(ApiErrorCode.PART_SIZE_MISMATCH,
                    "Sum of part sizes %d does not match totalBytes %d".formatted(
                            sum, session.getTotalBytes()));
        }
    }

    /**
     * Terminal-отказ (T3.4): домен переход {@code CREATED -> FAILED} с
     * failure_code пишется в текущей транзакции и коммитится — бизнес-ошибка
     * пробрасывается наружу только после коммита через {@code pendingError},
     * иначе {@code @Transactional} rollback-ил бы запись об отказе.
     */
    private Attempt failAndCleanup(UploadSession session, FailureCode code, ApiException error) {
        session.fail(code, Instant.now());
        uploadSessionRepository.save(session);
        cleanupQuietly(session);
        log.info("Session {} failed terminally: {}", session.getId(), code);
        return new Attempt(null, false, error);
    }

    /**
     * Очистка после terminal-отказа: объект удаляется, незавершённая
     * multipart-загрузка абортится. Сбой очистки не перекрывает бизнес-ошибку —
     * брошенные части дочистит TTL-эпик.
     */
    private void cleanupQuietly(UploadSession session) {
        try {
            storageGateway.deleteObject(session.getBucket(), session.getStoragePath());
        } catch (RuntimeException cleanupFailure) {
            log.warn("Object cleanup failed for session {}: {}",
                    session.getId(), cleanupFailure.toString());
        }
        try {
            storageGateway.abortMultipartUpload(
                    session.getBucket(), session.getStoragePath(), session.getUploadId());
        } catch (RuntimeException cleanupFailure) {
            log.warn("Multipart cleanup failed for session {}: {}",
                    session.getId(), cleanupFailure.toString());
        }
    }

    /**
     * Итог попытки complete внутри транзакции: {@code video} + признак первого
     * завершения, либо terminal-ошибка, пробрасываемая после коммита.
     */
    private record Attempt(Video video, boolean created, ApiException pendingError) {
    }
}
