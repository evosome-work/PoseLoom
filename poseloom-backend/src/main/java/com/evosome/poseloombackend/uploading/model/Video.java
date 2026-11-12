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
 * Видеоролик, созданный по завершённой {@link UploadSession}: файл в хранилище
 * прошёл проверки размера, контрольной суммы и контейнера.
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    /** Владелец ролика. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    /** Сессия, по завершении которой создан ролик. */
    @Column(name = "upload_session_id", nullable = false, updatable = false)
    private UUID uploadSessionId;

    /** Имя файла для отображения на клиенте. */
    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;

    /** Ключ объекта в хранилище, копия {@link UploadSession#getStoragePath()}. */
    @Column(name = "storage_path", nullable = false, updatable = false)
    private String storagePath;

    /** Фактический размер объекта в хранилище. */
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    /** SHA-256 файла, совпавший с заявленным при создании сессии. */
    @Column(name = "checksum_sha256", nullable = false, updatable = false, length = 64)
    private String checksumSha256;

    /** Контейнер, определённый по сигнатуре {@code ftyp}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "container", nullable = false, updatable = false)
    private VideoContainer container;

    /** Видеокодек; {@code null}. */
    @Column(name = "codec")
    private String codec;

    /** Длительность в секундах; {@code null} до извлечения медиаданных. */
    @Column(name = "duration_sec")
    private Integer durationSec;

    /** Разрешение (ширина x высота); {@code null} до извлечения медиаданных. */
    @Column(name = "resolution")
    private String resolution;

    /** Момент создания записи. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Момент успешного завершения загрузки. */
    @Column(name = "uploaded_at", nullable = false)
    @ToString.Include
    private Instant uploadedAt;
}
