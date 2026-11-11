package com.evosome.poseloombackend.uploading.model;

/**
 * Статус сессии multipart-загрузки.
 *
 * <p>Объём значений = CHECK {@code chk_upload_sessions_status} из
 * {@code V0.1-alpha__upload_core_schema.sql}, т.е. только три статуса эпика 001:
 * {@code UPLOADING}, {@code PAUSED}, {@code UPLOADED}, {@code VALIDATING}, {@code EXPIRED}
 * описаны в карточке домена и в целевой 8-статусной модели, но вынесены за рамки эпика
 * (см. diploma_v2 :: ER Deviations, Upload Flow «Состояния»): {@code UPLOADING} невозможен
 * без обратной связи от хранилища, {@code PAUSED} — эпик 002, {@code EXPIRED} — TTL-эпик.
 *
 * <p>Хранится как {@code varchar} ({@link EnumType#STRING}), не как {@code smallint}:
 * строковый литерал читаем в SELECT и логах и не требует маппинга, поэтому расширение
 * списка статусов удешевляется (решение от 07.11, diploma_v2 :: Context).
 * Терминальные состояния в эпике: {@link #COMPLETED}, {@link #FAILED}.
 */
public enum SessionStatus {

    /** Сессия и multipart-upload созданы, части можно загружать. */
    CREATED,

    /** Объект собран и проверен, {@link Video} создана. */
    COMPLETED,

    /** Неустранимая ошибка: объект и части удалены, {@link Video} не создаётся. */
    FAILED
}
