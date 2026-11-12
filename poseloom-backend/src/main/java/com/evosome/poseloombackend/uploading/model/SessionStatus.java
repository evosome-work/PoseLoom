package com.evosome.poseloombackend.uploading.model;

/**
 * Статус сессии multipart-загрузки.
 * 
 * <p>Хранится как {@code varchar} ({@link EnumType#STRING}), не как {@code smallint}:
 * строковый литерал читаем в SELECT и логах и не требует маппинга, поэтому расширение
 * списка статусов удешевляется.
 * Терминальные состояния: {@link #COMPLETED}, {@link #FAILED}.
 */
public enum SessionStatus {

    /** Сессия и multipart-upload созданы, части можно загружать. */
    CREATED,

    /** Объект собран и проверен, {@link Video} создана. */
    COMPLETED,

    /** Неустранимая ошибка: объект и части удалены, {@link Video} не создаётся. */
    FAILED
}
