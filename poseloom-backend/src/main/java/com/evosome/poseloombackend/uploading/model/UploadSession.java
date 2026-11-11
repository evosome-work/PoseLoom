package com.evosome.poseloombackend.uploading.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * Персистентная модель сессии chunked (multipart) загрузки видеофайла —
 * карточка домена
 * {@code diploma_v2 :: Shared/Domain/Entities/UploadSession}, таблица
 * {@code upload_sessions}
 * из {@code V0.1-alpha__upload_core_schema.sql} (T0.2 / T2.1, T2.2).
 *
 * <p>
 * Модель анемичная: поля + геттеры/сеттеры, переходы и инварианты живут в
 * application-слое
 * (этап 3.x, T3.7 «Машина состояний»). Инвариант парности
 * {@code status/failure_code} до этих
 * пор соблюдает только CHECK {@code chk_upload_sessions_failure_code_pair}:
 * запись
 * {@code COMPLETED} с непустым кодом или {@code FAILED} без кода даст
 * {@code DataIntegrityViolationException} — по правилу ER Deviations это 500
 * (баг кода),
 * а не бизнес-ошибка.
 *
 * <p>
 * Маппинг следует DDL один-в-один, т.к.
 * {@code spring.jpa.hibernate.ddl-auto=validate}:
 * {@code uuid} → {@link UUID}, {@code varchar} → {@link String},
 * {@code int8}/{@code int4} →
 * {@code long}/{@code int}, {@code timestamptz} → {@link Instant} (не
 * {@code LocalDateTime}:
 * он маппится в {@code timestamp} без зоны и валидацию бы уронил). Точность
 * {@code timestamptz}
 * в PostgreSQL — микросекунды, значения с наносекундами округляются при записи.
 *
 * <p>
 * {@code uploaded_bytes} (прогресс) и {@code last_activity_at} (TTL)
 * отсутствуют: их нет
 * в схеме эпика 001, они относятся к эпикам 002 и TTL.
 */
@Entity
@Table(name = "upload_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UploadSession {

    /**
     * Имя свойства {@link #failureCode} — для явного {@code @Convert} и производных
     * запросов.
     */
    public static final String FAILURE_CODE_PROPERTY = "failureCode";

    /**
     * {@code uuid NOT NULL DEFAULT gen_random_uuid()}.
     *
     * <p>
     * Идентификатор генерируется приложением, а не ждёт DB-default:
     * {@code storage_path}
     * строится из {@code sessionId} ({@code sessions/{sessionId}/source}) ещё до
     * INSERT,
     * и {@code JpaRepository#save} обязан идти как {@code persist}, а не
     * {@code merge}
     * с предварительным SELECT. DB-default остаётся фолбэком для raw-SQL (тесты
     * миграции).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    /**
     * {@code uploader_id uuid NOT NULL} — ссылка на инициатора загрузки.
     * FK отсутствует намеренно: аутентификация вне эпика, пользователь из заглушки
     * {@code X-User-Id} (ER Deviations). Неизменяем после создания.
     */
    @Column(name = "uploader_id", nullable = false, updatable = false)
    private UUID uploaderId;

    /**
     * {@code status varchar NOT NULL} + CHECK. Только три значения эпика 001,
     * см. {@link SessionStatus}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ToString.Include
    private SessionStatus status;

    /**
     * {@code bucket varchar NOT NULL} — бакет Silo/MinIO
     * ({@code poseloom.storage.s3.bucket}).
     */
    @Column(name = "bucket", nullable = false, updatable = false)
    private String bucket;

    /**
     * {@code storage_path varchar NOT NULL UNIQUE} — ключ объекта
     * {@code sessions/{sessionId}/source}.
     * Не зависит от имени файла и неизменяем: на него подписываются presigned URL и
     * он же
     * копируется в {@link Video#getStoragePath()}.
     */
    @Column(name = "storage_path", nullable = false, updatable = false)
    private String storagePath;

    /**
     * {@code upload_id varchar NOT NULL} — идентификатор multipart-загрузки в
     * хранилище.
     * Заполняется до INSERT: {@code CreateMultipartUpload} выполняется до ответа
     * клиенту,
     * поэтому активная сессия всегда имеет непустой {@code uploadId} (инвариант
     * карточки домена).
     */
    @Column(name = "upload_id", nullable = false, updatable = false)
    private String uploadId;

    /**
     * {@code original_filename varchar NOT NULL} — исходное имя файла, только для
     * отображения.
     */
    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;

    /**
     * {@code total_bytes int8 NOT NULL} + CHECK {@code 0 < x <= 104857600} (100
     * МиБ).
     * Неизменяем; границы проверяет сервис (T3.1, коды
     * {@code FILE_TOO_LARGE}/{@code INVALID_FILE_SIZE}).
     */
    @Column(name = "total_bytes", nullable = false, updatable = false)
    private long totalBytes;

    /**
     * {@code part_size int4 NOT NULL} — 10 МиБ (10485760), неизменяем.
     * Хранится, а не берётся из конфига, чтобы изменение
     * {@code poseloom.upload.part-size}
     * не сломало возобновление загрузки (config drift, решение от 07.11).
     */
    @Column(name = "part_size", nullable = false, updatable = false)
    private int partSize;

    /**
     * {@code part_count int4 NOT NULL} — {@code ceil(total_bytes / part_size)},
     * максимум 10.
     * Рассчитывается сервером один раз при создании сессии (T0.1) и к прогрессу
     * загрузки
     * отношения не имеет; ограничивает диапазон presign-а ({@code 1..partCount}).
     */
    @Column(name = "part_count", nullable = false, updatable = false)
    private int partCount;

    /**
     * {@code checksum_sha256 varchar(64) NOT NULL} — канон T0.1: SHA-256 всего
     * файла
     * (не multipart ETag) в нижнем регистре, 64 hex-символа.
     */
    @Column(name = "checksum_sha256", nullable = false, updatable = false, length = 64)
    private String checksumSha256;

    /**
     * {@code failure_code varchar NULL} — причина {@link SessionStatus#FAILED};
     * «отказа нет» = {@link FailureCode#NONE} в Java и SQL {@code NULL} в БД
     * ({@link FailureCodeConverter}).
     */
    @Convert(converter = FailureCodeConverter.class)
    @Column(name = "failure_code")
    private FailureCode failureCode;

    /**
     * {@code created_at timestamptz NOT NULL DEFAULT now()} — создание сессии,
     * неизменяем.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * {@code updated_at timestamptz NOT NULL DEFAULT now()} — последнее изменение
     * записи;
     * обновляется явным сеттером вместе с переходом статуса (bulk-UPDATE по
     * {@code status}/{@code failure_code} запрещён ER Deviations).
     */
    @Column(name = "updated_at", nullable = false)
    @ToString.Include
    private Instant updatedAt;

    /**
     * {@code completed_at timestamptz NULL} — момент
     * {@link SessionStatus#COMPLETED};
     * {@code null}, пока завершение не наступило (FAILED тоже оставляет
     * {@code null}).
     */
    @Column(name = "completed_at")
    private Instant completedAt;
}
