package com.evosome.poseloombackend.uploading.model;

/**
 * Причина терминального отказа загрузки.
 *
 * <p>Список кодов закрыт и совпадает со вторым CHECK из DDL
 * ({@code chk_upload_sessions_failure_code_value}): {@link #SIZE_MISMATCH},
 * {@link #CHECKSUM_MISMATCH}, {@link #UNSUPPORTED_CONTAINER}. Retryable-ошибки
 * ({@code MISSING_PARTS}, {@code PART_SIZE_MISMATCH}, transient {@code STORAGE_ERROR})
 * кода не имеют: сессия остаётся в {@link SessionStatus#CREATED} (решение T0.1).
 *
 * <p>{@link #NONE} — null-object «отказа нет» (diploma_v2 :: ER Deviations). В Java-домене
 * поле никогда не {@code null}, что и сохраняет инвариант парности
 * {@code status = 'FAILED' <=> failure_code IS NOT NULL}. В БД {@code NONE}
 * сериализуется в {@code NULL}: сентинел-строка {@code 'NO_FAILURE'} в схему сознательно
 * не вводился. Маппинг — {@link FailureCodeConverter}.
 */
public enum FailureCode {

    /** Отсутствует: в БД — {@code NULL}, допустимо при любом статусе, кроме {@code FAILED}. */
    NONE,

    /** {@code HeadObject.ContentLength} не совпал с заявленным {@code totalBytes}. */
    SIZE_MISMATCH,

    /** SHA-256 пересчитанного сервером объекта не совпал с заявленным при создании сессии. */
    CHECKSUM_MISMATCH,

    /** Сигнатура ISO BMFF ({@code ftyp}) или major brand не распознаны. */
    UNSUPPORTED_CONTAINER
}
