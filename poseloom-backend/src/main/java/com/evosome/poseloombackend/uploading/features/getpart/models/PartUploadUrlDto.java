package com.evosome.poseloombackend.uploading.feature.getpart.models;

import java.time.Instant;

/**
 * Presigned {@code UploadPart} URL по
 * {@code components/schemas/PartUploadUrl.yml}: credentials хранилища клиенту
 * не выдаются, истёкший URL отклоняется хранилищем — клиент запрашивает новый.
 *
 * @param url        presigned URL для {@code PUT} части напрямую в хранилище
 * @param partNumber номер части, для которой выдан URL
 * @param expiresAt  момент истечения URL
 */
public record PartUploadUrlDto(String url, int partNumber, Instant expiresAt) {
}
