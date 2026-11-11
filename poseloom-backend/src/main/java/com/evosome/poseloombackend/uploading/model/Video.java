package com.evosome.poseloombackend.uploading.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Персистентная модель корректно загруженного видеоролика — карточка домена
 * {@code diploma_v2 :: Shared/Domain/Entities/Video}, таблица {@code videos} из
 * {@code V0.1-alpha__upload_core_schema.sql} (T0.2 / T2.2).
 *
 * <p>
 * Зарождается только как результат {@link UploadSession} в статусе
 * {@link SessionStatus#COMPLETED}: файл физически существует в хранилище, его
 * размер и
 * SHA-256 сверены, сигнатура контейнера распознана. Связь 1:0..1 замыкает
 * {@code upload_session_id uuid NOT NULL UNIQUE} + FK (решение T0.1 закрыло
 * открытый вопрос
 * карточки «нужен ли upload_session_id»): UNIQUE — финальная защита от дубля
 * при
 * параллельных {@code complete} (T3.5).
 *
 * <p>
 * Отступления от доменной карточки, зафиксированные в ER Deviations: VO
 * {@code VideoFormat}
 * разложен на {@link #container} + {@link #codec} (nullable до ffprobe),
 * инварианты
 * {@code duration_sec > 0} и {@code resolution != null} перенесены в эпик 002,
 * поэтому
 * {@link #durationSec} и {@link #resolution} — nullable. Таблицы {@code users}
 * в эпике нет,
 * {@link #ownerId} — UUID без FK (заглушка {@code X-User-Id}).
 *
 * <p>
 * {@link #uploadSessionId} — скаляр, а не {@code @OneToOne}-ассоциация:
 * lazy-ссылка с
 * {@code @JoinColumn} без bytecode enhancement дала бы дополнительный SELECT на
 * каждую
 * загрузку, а операции идемпотентного {@code complete} всё равно нужен запрос
 * «Video по сессии» — со скаляром он тривиален
 * ({@code VideoRepository#findByUploadSessionId}).
 * Целостность связи обеспечивает схема (FK + UNIQUE).
 */
@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Video {

    /**
     * {@code uuid NOT NULL DEFAULT gen_random_uuid()}, генерируется приложением.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    /**
     * {@code owner_id uuid NOT NULL} — владелец ролика (копия {@code uploader_id}
     * сессии);
     * FK не ставится: user stub. Индекс {@code idx_videos_owner}.
     */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    /**
     * {@code upload_session_id uuid NOT NULL UNIQUE} + FK
     * {@code ON DELETE RESTRICT} —
     * сессия, при завершении которой создан ролик. Неизменяем: роль 1:0..1
     * закреплена
     * за схемой, а не за JPA-ассоциацией.
     */
    @Column(name = "upload_session_id", nullable = false, updatable = false)
    private UUID uploadSessionId;

    /**
     * {@code original_filename varchar NOT NULL} — имя для отображения на клиенте.
     */
    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;

    /**
     * {@code storage_path varchar NOT NULL UNIQUE} — ключ объекта, копия
     * {@link UploadSession#getStoragePath()}.
     */
    @Column(name = "storage_path", nullable = false, updatable = false)
    private String storagePath;

    /**
     * {@code size_bytes int8 NOT NULL} — фактический размер объекта
     * ({@code HeadObject.ContentLength}).
     */
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    /**
     * {@code checksum_sha256 varchar(64) NOT NULL} — SHA-256, совпавший с
     * заявленным при создании сессии.
     */
    @Column(name = "checksum_sha256", nullable = false, updatable = false, length = 64)
    private String checksumSha256;

    /**
     * {@code container varchar NOT NULL} + CHECK {@code ('MP4','MOV')} — часть VO
     * {@code VideoFormat}, определённая без ffprobe по {@code ftyp}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "container", nullable = false, updatable = false)
    private VideoContainer container;

    /**
     * {@code codec varchar NULL} — вторая часть VO {@code VideoFormat}; заполняется
     * ffprobe в эпике 002, до этого — {@code null}.
     */
    @Column(name = "codec")
    private String codec;

    /**
     * {@code duration_sec int4 NULL} — длительность; появится с медиаданными (эпик
     * 002).
     */
    @Column(name = "duration_sec")
    private Integer durationSec;

    /**
     * {@code resolution varchar NULL} — VO {@code Resolution} (ширина x высота),
     * эпик 002.
     */
    @Column(name = "resolution")
    private String resolution;

    /**
     * {@code created_at timestamptz NOT NULL DEFAULT now()} — создание записи,
     * неизменяем.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * {@code uploaded_at timestamptz NOT NULL DEFAULT now()} — успешное завершение
     * загрузки.
     */
    @Column(name = "uploaded_at", nullable = false)
    @ToString.Include
    private Instant uploadedAt;
}
