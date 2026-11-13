-- Epic 001. Video Uploading Core — схема PostgreSQL (Tasks T0.2 / T2.1)
-- Источник: diploma_v2 :: ER/ER PlantUML + ER Deviations.
-- Конвенции: snake_case, UUID через gen_random_uuid() (нативная в PG13+), все timestamps — timestamptz.
-- Таблица users отсутствует намеренно: аутентификация вне эпика, пользователь из заглушки X-User-Id,
-- поэтому uploader_id / owner_id — UUID без FK.
-- Отступление от ER: checksum_sha256 — varchar(64) вместо char(64) (bpchar ломает
-- spring.jpa.hibernate.ddl-auto=validate для String-поля); длина 64 зафиксирована в типе.

CREATE TABLE upload_sessions (
    id                uuid         NOT NULL DEFAULT gen_random_uuid(),
    uploader_id       uuid         NOT NULL,                  -- FK не ставится: user stub (X-User-Id)
    status            varchar      NOT NULL,
    bucket            varchar      NOT NULL,
    storage_path      varchar      NOT NULL,                  -- ключ объекта sessions/{sessionId}/source
    upload_id         varchar      NOT NULL,                  -- CreateMultipartUpload выполнен до INSERT
    original_filename varchar      NOT NULL,
    total_bytes       bigint       NOT NULL,
    part_size         int          NOT NULL,                  -- 10485760 (10 MiB), неизменяемый
    part_count        int          NOT NULL,                  -- ceil(total_bytes / part_size), <= 10
    checksum_sha256   varchar(64)  NOT NULL,                  -- канон T0.1: SHA-256 вместо доменного MD5
    failure_code      varchar,                                -- NOT NULL ровно при status = 'FAILED'
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    completed_at      timestamptz,

    CONSTRAINT pk_upload_sessions PRIMARY KEY (id),

    CONSTRAINT uk_upload_sessions_storage_path UNIQUE (storage_path),

    -- В эпике 3 статуса из 8 целевых (UPLOADING/PAUSED/UPLOADED/VALIDATING/EXPIRED — вне эпика).
    CONSTRAINT chk_upload_sessions_status CHECK (status IN ('CREATED', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk_upload_sessions_total_bytes CHECK (total_bytes > 0 AND total_bytes <= 104857600),

    -- Инвариант парности: failure_code IS NOT NULL <=> status = 'FAILED'.
    CONSTRAINT chk_upload_sessions_failure_code_pair
        CHECK ((status = 'FAILED') = (failure_code IS NOT NULL)),

    -- Закрытый список кодов; сентинел 'NO_FAILURE' не вводится, NULL = «отказа нет».
    CONSTRAINT chk_upload_sessions_failure_code_value
        CHECK (failure_code IS NULL
               OR failure_code IN ('SIZE_MISMATCH', 'CHECKSUM_MISMATCH', 'UNSUPPORTED_CONTAINER'))
);

-- uploaded_bytes (прогресс) и last_activity_at (TTL) в схеме нет: эпик 002 / TTL-эпик.
CREATE INDEX idx_upload_sessions_uploader_status ON upload_sessions (uploader_id, status);

CREATE TABLE videos (
    id                uuid         NOT NULL DEFAULT gen_random_uuid(),
    owner_id          uuid         NOT NULL,                  -- FK не ставится: user stub
    upload_session_id uuid         NOT NULL,                  -- связь 1:1 с сессией (решение T0.1)
    original_filename varchar      NOT NULL,
    storage_path      varchar      NOT NULL,                  -- копия ключа объекта из сессии
    size_bytes        bigint       NOT NULL,
    checksum_sha256   varchar(64)  NOT NULL,
    container         varchar      NOT NULL,                  -- VideoFormat (VO) = container + codec
    codec             varchar,                                -- nullable до ffprobe (эпик 002)
    duration_sec      int,                                    -- nullable; инвариант > 0 — эпик 002
    resolution        varchar,                                -- VO Resolution (W x H); nullable, эпик 002
    created_at        timestamptz  NOT NULL DEFAULT now(),
    uploaded_at       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_videos PRIMARY KEY (id),

    -- UNIQUE замыкает 1:0..1 и является финальной защитой от дубля при параллельном complete (T3.5).
    CONSTRAINT uk_videos_upload_session UNIQUE (upload_session_id),

    CONSTRAINT uk_videos_storage_path UNIQUE (storage_path),

    CONSTRAINT fk_videos_upload_session
        FOREIGN KEY (upload_session_id) REFERENCES upload_sessions (id) ON DELETE RESTRICT,

    CONSTRAINT chk_videos_container CHECK (container IN ('MP4', 'MOV'))
);

CREATE INDEX idx_videos_owner ON videos (owner_id);
