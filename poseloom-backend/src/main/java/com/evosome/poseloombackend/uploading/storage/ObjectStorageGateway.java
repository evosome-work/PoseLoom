package com.evosome.poseloombackend.uploading.storage;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.evosome.poseloombackend.uploading.exception.StorageException;

/**
 * Порт к S3-совместимому объектному хранилищу: сервисы работают
 * только через него, AWS SDK или другой SDK не идет наружу.
 * Все методы бросают {@link StorageException} при сбое хранилища;
 * transient-характер позволяет повторить позже, сессия при этом не разрушается.
 */
public interface ObjectStorageGateway {

    /**
     * Инициирует multipart-загрузку.
     *
     * @return {@code uploadId} для последующих presign/complete/abort
     */
    String createMultipartUpload(String bucket, String key);

    /**
     * Подписывает {@code UploadPart} публичным endpoint (presigned URL для прямого
     * PUT из клиента, credentials хранилища не выдаются).
     */
    PresignedPartUrl presignUploadPart(String bucket, String key, String uploadId,
                                       int partNumber, Duration ttl);

    /**
     * Возвращает все части незавершённой multipart-загрузки со всех страниц
     * {@code ListParts} ({@code isTruncated}/{@code nextPartNumberMarker}).
     */
    List<StoredPart> listAllParts(String bucket, String key, String uploadId);

    /**
     * Собирает объект из частей с ETag, полученными из {@link #listAllParts}.
     */
    void completeMultipartUpload(String bucket, String key, String uploadId, List<StoredPart> parts);

    /**
     * {@code HeadObject.ContentLength} собранного объекта.
     */
    long headObjectSize(String bucket, String key);

    /**
     * Диапазон {@code [fromInclusive, toInclusive]} объекта через Range GET
     * (первые 12 байт для проверки {@code ftyp}).
     */
    byte[] rangeGet(String bucket, String key, long fromInclusive, long toInclusive);

    /**
     * Потоковый читатель всего объекта для SHA-256; закрывает ответ хранилища
     * закрением returned-потока. Вызывать на streaming-клиенте с отдельным
     * socket-timeout (S3UploadingClient).
     */
    InputStream openObjectStream(String bucket, String key);

    /**
     * Прерывает незавершённую multipart-загрузку; отсутствующую считает уже
     * прерванной (idempotent cleanup).
     */
    void abortMultipartUpload(String bucket, String key, String uploadId);

    /**
     * Удаляет объект; отсутствующего считать удалённым (idempotent cleanup).
     */
    void deleteObject(String bucket, String key);

    /**
     * Presigned URL части и момент его истечения.
     */
    record PresignedPartUrl(String url, Instant expiresAt) {
    }

    /**
     * Часть в хранилище: номер, размер в байтах, ETag для complete.
     */
    record StoredPart(int partNumber, long size, String etag) {
    }
}
