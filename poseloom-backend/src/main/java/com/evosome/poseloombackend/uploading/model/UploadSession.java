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
 * Сессия multipart-загрузки видеофайла: параметры загрузки и её текущее состояние.
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    /** Пользователь, инициировавший загрузку. */
    @Column(name = "uploader_id", nullable = false, updatable = false)
    private UUID uploaderId;

    /** Статус загрузки (см. {@link SessionStatus}). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ToString.Include
    private SessionStatus status;

    /** Бакет объектного хранилища под файл. */
    @Column(name = "bucket", nullable = false, updatable = false)
    private String bucket;

    /**
     * Ключ объекта в хранилище; неизменяем, используется для presigned URL и
     * копируется в {@link Video#getStoragePath()}.
     */
    @Column(name = "storage_path", nullable = false, updatable = false)
    private String storagePath;

    /** Идентификатор multipart-загрузки в хранилище; известен до создания записи. */
    @Column(name = "upload_id", nullable = false, updatable = false)
    private String uploadId;

    /** Исходное имя файла — только для отображения. */
    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;

    /** Заявленный размер файла в байтах. */
    @Column(name = "total_bytes", nullable = false, updatable = false)
    private long totalBytes;

    /** Размер части в байтах. */
    @Column(name = "part_size", nullable = false, updatable = false)
    private int partSize;

    /** Число частей загрузки; ограничивает диапазон presign-а. */
    @Column(name = "part_count", nullable = false, updatable = false)
    private int partCount;

    /** SHA-256 всего файла: 64 hex-символа в нижнем регистре. */
    @Column(name = "checksum_sha256", nullable = false, updatable = false, length = 64)
    private String checksumSha256;

    /**
     * Причина отказа; {@link FailureCode#NONE} (в БД — {@code NULL}), пока отказа нет.
     */
    @Convert(converter = FailureCodeConverter.class)
    @Column(name = "failure_code")
    private FailureCode failureCode;

    /** Момент создания сессии. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Момент последнего изменения записи. */
    @Column(name = "updated_at", nullable = false)
    @ToString.Include
    private Instant updatedAt;

    /** Момент завершения загрузки; {@code null} до {@link SessionStatus#COMPLETED}. */
    @Column(name = "completed_at")
    private Instant completedAt;
}
