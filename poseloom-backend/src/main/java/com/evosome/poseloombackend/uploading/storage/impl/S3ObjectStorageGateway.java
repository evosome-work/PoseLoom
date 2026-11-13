package com.evosome.poseloombackend.uploading.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.evosome.poseloombackend.client.S3UploadingClient;
import com.evosome.poseloombackend.uploading.exception.StorageException;
import com.evosome.poseloombackend.uploading.storage.ObjectStorageGateway;
import com.evosome.poseloombackend.uploading.storage.ObjectStorageGateway.PresignedPartUrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

/**
 * S3-адаптер {@link ObjectStorageGateway} (Tasks T2.3): metadata-операции идут
 * через общий {@code s3Client}, потоковое чтение SHA-256 — через
 * {@code s3StreamingClient} с увеличенным socket-timeout, presign — через
 * {@code S3Presigner} публичным endpoint. Сбои SDK оборачиваются в
 * {@link StorageException}: типы/сообщения AWS SDK наружу не протекают,
 * presigned URL в логи не пишутся.
 */
@Component
public class S3ObjectStorageGateway implements ObjectStorageGateway {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorageGateway.class);

    private static final String GENERIC_FAILURE = "Object storage operation failed";

    private final S3Client s3Client;
    private final S3Client s3StreamingClient;
    private final S3Presigner s3Presigner;

    public S3ObjectStorageGateway(S3Client s3Client,
                                  @Qualifier(S3UploadingClient.STREAMING_CLIENT) S3Client s3StreamingClient,
                                  S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3StreamingClient = s3StreamingClient;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String createMultipartUpload(String bucket, String key) {
        try {
            return s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())
                    .uploadId();
        } catch (SdkException e) {
            throw storageFailure("CreateMultipartUpload", e);
        }
    }

    @Override
    public PresignedPartUrl presignUploadPart(String bucket, String key, String uploadId,
                                              int partNumber, Duration ttl) {
        try {
            PresignedUploadPartRequest presigned = s3Presigner.presignUploadPart(
                    UploadPartPresignRequest.builder()
                            .signatureDuration(ttl)
                            .uploadPartRequest(rb -> rb.bucket(bucket)
                                    .key(key)
                                    .uploadId(uploadId)
                                    .partNumber(partNumber))
                            .build());
            return new PresignedPartUrl(presigned.url().toString(), presigned.expiration());
        } catch (SdkException e) {
            throw storageFailure("presign UploadPart", e);
        }
    }

    @Override
    public List<StoredPart> listAllParts(String bucket, String key, String uploadId) {
        List<StoredPart> parts = new ArrayList<>();
        try {
            Integer marker = null;
            boolean truncated = true;
            while (truncated) {
                ListPartsRequest.Builder request = ListPartsRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId);
                if (marker != null) {
                    request.partNumberMarker(marker);
                }
                ListPartsResponse response = s3Client.listParts(request.build());
                for (Part part : response.parts()) {
                    parts.add(new StoredPart(part.partNumber(), part.size(), part.eTag()));
                }
                truncated = Boolean.TRUE.equals(response.isTruncated());
                marker = response.nextPartNumberMarker();
            }
        } catch (SdkException e) {
            throw storageFailure("ListParts", e);
        }
        return parts;
    }

    @Override
    public void completeMultipartUpload(String bucket, String key, String uploadId,
                                        List<StoredPart> parts) {
        try {
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(parts.stream()
                                    .map(part -> CompletedPart.builder()
                                            .partNumber(part.partNumber())
                                            .eTag(part.etag())
                                            .build())
                                    .toList())
                            .build())
                    .build());
        } catch (SdkException e) {
            throw storageFailure("CompleteMultipartUpload", e);
        }
    }

    @Override
    public long headObjectSize(String bucket, String key) {
        try {
            return s3Client.headObject(HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())
                    .contentLength();
        } catch (SdkException e) {
            throw storageFailure("HeadObject", e);
        }
    }

    @Override
    public byte[] rangeGet(String bucket, String key, long fromInclusive, long toInclusive) {
        try (ResponseInputStream<?> response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .range("bytes=%d-%d".formatted(fromInclusive, toInclusive))
                .build())) {
            return response.readAllBytes();
        } catch (NoSuchKeyException e) {
            throw new StorageException("Range GET: object is missing", e);
        } catch (SdkException | IOException e) {
            throw storageFailure("Range GET", e);
        }
    }

    @Override
    public InputStream openObjectStream(String bucket, String key) {
        try {
            return s3StreamingClient.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new StorageException("GET object: object is missing", e);
        } catch (SdkException e) {
            throw storageFailure("GET object", e);
        }
    }

    @Override
    public void abortMultipartUpload(String bucket, String key, String uploadId) {
        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build());
        } catch (NoSuchUploadException alreadyAborted) {
            log.info("Multipart upload is already aborted: key={}", key);
        } catch (SdkException e) {
            throw storageFailure("AbortMultipartUpload", e);
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (SdkException e) {
            throw storageFailure("DeleteObject", e);
        }
    }

    private static StorageException storageFailure(String operation, Throwable cause) {
        log.warn("Storage operation {} failed: {}", operation, cause.toString());
        return new StorageException(GENERIC_FAILURE, cause);
    }
}
