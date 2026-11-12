package com.evosome.poseloombackend.uploading.model;

/**
 * Причины терминального отказа загрузки.
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
