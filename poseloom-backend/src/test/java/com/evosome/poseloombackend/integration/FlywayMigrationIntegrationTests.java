package com.evosome.poseloombackend.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Epic 001, Tasks T2.1: V1 Flyway-миграция накатывается на чистую PostgreSQL без ошибок.
 * Источник схемы: diploma_v2 :: ER/ER PlantUML + ER Deviations.
 * spring.jpa.hibernate.ddl-auto=validate здесь не проверяется — JPA-сущности появятся в T2.2.
 */
@Testcontainers
class FlywayMigrationIntegrationTests {

    private static final String POSTGRES_IMAGE = System.getenv().getOrDefault("POSTGRES_IMAGE", "postgres:18");

    private static final String CHECK_VIOLATION = "23514";
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String NOT_NULL_VIOLATION = "23502";
    private static final String FK_VIOLATION = "23503";

    private static final String CHECKSUM = "9f2c8a6e5b1d3f7a0c2e4b6d8f1a3c5e7b9d1f3a5c7e9b1d3f5a7c9e1b3d5f7a";

    private static final String SESSION_INSERT = """
            insert into upload_sessions (id, uploader_id, status, bucket, storage_path, upload_id,
                                         original_filename, total_bytes, part_size, part_count,
                                         checksum_sha256, failure_code)
            values (?, ?, ?, 'videos', ?, ?, 'squat.mp4', ?, 10485760, ?, ?, ?)
            """;

    private static final String VIDEO_INSERT = """
            insert into videos (id, owner_id, upload_session_id, original_filename, storage_path,
                                size_bytes, checksum_sha256, container, codec, duration_sec, resolution)
            values (?, ?, ?, 'squat.mp4', ?, ?, ?, ?, ?, ?, ?)
            """;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE));

    static Connection connection;

    @BeforeAll
    static void migrate() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        connection.close();
    }

    @Test
    @DisplayName("V1 — единственная применённая миграция, и она успешна")
    void appliedV1WithoutErrors() throws SQLException {
        assertThat(column("""
                select version || ' ' || description || ' ' || success
                from flyway_schema_history
                """))
                .containsExactly("1 upload core schema true");
    }

    @Test
    @DisplayName("upload_sessions: колонки, типы и nullability соответствуют ER")
    void uploadSessionsColumnsMatchErDiagram() throws SQLException {
        assertThat(tableShape("upload_sessions")).containsExactly(
                "id uuid NO",
                "uploader_id uuid NO",
                "status character varying NO",
                "bucket character varying NO",
                "storage_path character varying NO",
                "upload_id character varying NO",
                "original_filename character varying NO",
                "total_bytes bigint NO",
                "part_size integer NO",
                "part_count integer NO",
                "checksum_sha256 character varying(64) NO",
                "failure_code character varying YES",
                "created_at timestamp with time zone NO",
                "updated_at timestamp with time zone NO",
                "completed_at timestamp with time zone YES");
    }

    @Test
    @DisplayName("videos: колонки, типы и nullability соответствуют ER")
    void videosColumnsMatchErDiagram() throws SQLException {
        assertThat(tableShape("videos")).containsExactly(
                "id uuid NO",
                "owner_id uuid NO",
                "upload_session_id uuid NO",
                "original_filename character varying NO",
                "storage_path character varying NO",
                "size_bytes bigint NO",
                "checksum_sha256 character varying(64) NO",
                "container character varying NO",
                "codec character varying YES",
                "duration_sec integer YES",
                "resolution character varying YES",
                "created_at timestamp with time zone NO",
                "uploaded_at timestamp with time zone NO");
    }

    @Test
    @DisplayName("Constraints и индексы из T0.2 на месте")
    void constraintsAndIndexesMatchErDiagram() throws SQLException {
        assertThat(column("select conname from pg_constraint where conrelid = 'upload_sessions'::regclass order by conname"))
                .containsExactly(
                        "chk_upload_sessions_failure_code_pair",
                        "chk_upload_sessions_failure_code_value",
                        "chk_upload_sessions_status",
                        "chk_upload_sessions_total_bytes",
                        "pk_upload_sessions",
                        "uk_upload_sessions_storage_path");

        assertThat(column("select conname from pg_constraint where conrelid = 'videos'::regclass order by conname"))
                .containsExactly(
                        "chk_videos_container",
                        "fk_videos_upload_session",
                        "pk_videos",
                        "uk_videos_storage_path",
                        "uk_videos_upload_session");

        assertThat(column("select indexname from pg_indexes where tablename = 'upload_sessions'"))
                .contains("idx_upload_sessions_uploader_status");
        assertThat(column("select indexname from pg_indexes where tablename = 'videos'"))
                .contains("idx_videos_owner");
    }

    @Test
    @DisplayName("PK через gen_random_uuid() и timestamptz с DEFAULT now() работают")
    void databaseGeneratedColumnsArePopulated() throws SQLException {
        UUID sessionId = insertSessionWithGeneratedId();
        assertThat(sessionId).isNotNull();
        assertThat(scalar("""
                select (created_at is not null and updated_at is not null and completed_at is null)::text
                from upload_sessions where id = ?
                """, sessionId)).isEqualTo("true");

        UUID videoId = insertVideoWithGeneratedId(sessionId, "sessions/" + sessionId + "/source", "MP4");
        assertThat(videoId).isNotNull();
        assertThat(scalar("""
                select (created_at is not null and uploaded_at is not null
                        and codec is null and duration_sec is null and resolution is null)::text
                from videos where id = ?
                """, videoId)).isEqualTo("true");
    }

    @Test
    @DisplayName("ER-инварианты защищены на уровне DDL")
    void schemaGuardsErInvariants() throws SQLException {
        UUID uploader = UUID.randomUUID();
        UUID session = UUID.randomUUID();

        // status: только CREATED / COMPLETED / FAILED (8-статусная модель — вне эпика).
        assertThat(insertSessionState(session, uploader, "UPLOADING", 73_400_320L, null))
                .isEqualTo(CHECK_VIOLATION);

        // total_bytes: 0 < x <= 100 MiB.
        assertThat(insertSessionState(session, uploader, "CREATED", 0L, null)).isEqualTo(CHECK_VIOLATION);
        assertThat(insertSessionState(session, uploader, "CREATED", 104_857_601L, null)).isEqualTo(CHECK_VIOLATION);

        // failure_code: NOT NULL ровно при status = 'FAILED'.
        assertThat(insertSessionState(session, uploader, "FAILED", 73_400_320L, null)).isEqualTo(CHECK_VIOLATION);
        assertThat(insertSessionState(session, uploader, "COMPLETED", 73_400_320L, "SIZE_MISMATCH"))
                .isEqualTo(CHECK_VIOLATION);
        // ... и закрытый список кодов: 'NO_FAILURE'-сентинел в схеме отсутствует.
        assertThat(insertSessionState(session, uploader, "FAILED", 73_400_320L, "STORAGE_ERROR"))
                .isEqualTo(CHECK_VIOLATION);
        assertThat(insertSessionState(session, uploader, "CREATED", 73_400_320L, "NO_FAILURE"))
                .isEqualTo(CHECK_VIOLATION);

        // uploader_id без FK, но NOT NULL (user stub).
        assertThat(sqlStateOf(SESSION_INSERT, session, null, "CREATED",
                "sessions/" + session + "/source", "multipart-upload-id", 73_400_320L, 7, CHECKSUM, null))
                .isEqualTo(NOT_NULL_VIOLATION);

        // Валидная FAILED-сессия проходит, иначе CHECK был бы слишком строгим.
        assertThat(insertSessionState(session, uploader, "FAILED", 73_400_320L, "CHECKSUM_MISMATCH")).isNull();

        // container: MP4 | MOV; codec / duration_sec / resolution — nullable до эпика 002.
        assertThat(insertVideoState(UUID.randomUUID(), uploader, session, "AVI")).isEqualTo(CHECK_VIOLATION);

        // videos.upload_session_id NOT NULL UNIQUE: связь 1:0..1, дубль при параллельном complete невозможен.
        String path = "sessions/" + session + "/source";
        assertThat(sqlStateOf(VIDEO_INSERT, UUID.randomUUID(), uploader, session, path,
                73_400_320L, CHECKSUM, "MP4", null, null, null)).isNull();
        assertThat(insertVideoState(UUID.randomUUID(), uploader, session, "MP4")).isEqualTo(UNIQUE_VIOLATION);
        assertThat(insertVideoState(UUID.randomUUID(), uploader, UUID.randomUUID(), "MOV")).isEqualTo(FK_VIOLATION);
    }

    private String insertSessionState(UUID id, UUID uploader, String status, long totalBytes, String failureCode) {
        return sqlStateOf(SESSION_INSERT, id, uploader, status, "sessions/" + id + "/source",
                "multipart-upload-id", totalBytes, (int) Math.ceil(totalBytes / 10_485_760.0), CHECKSUM, failureCode);
    }

    private String insertVideoState(UUID id, UUID owner, UUID sessionId, String container) {
        return sqlStateOf(VIDEO_INSERT, id, owner, sessionId, "sessions/" + sessionId + "/source",
                73_400_320L, CHECKSUM, container, null, null, null);
    }

    private UUID insertSessionWithGeneratedId() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                insert into upload_sessions (uploader_id, status, bucket, storage_path, upload_id,
                                             original_filename, total_bytes, part_size, part_count, checksum_sha256)
                values (?, 'CREATED', 'videos', ?, ?, 'squat.mp4', 73400320, 10485760, 7, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            UUID sessionId = UUID.randomUUID();
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, "sessions/" + sessionId + "/source");
            ps.setString(3, "multipart-upload-id");
            ps.setString(4, CHECKSUM);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getObject("id", UUID.class);
            }
        }
    }

    private UUID insertVideoWithGeneratedId(UUID sessionId, String storagePath, String container)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                insert into videos (owner_id, upload_session_id, original_filename, storage_path,
                                    size_bytes, checksum_sha256, container)
                values (?, ?, 'squat.mp4', ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, sessionId);
            ps.setString(3, storagePath);
            ps.setLong(4, 73_400_320L);
            ps.setString(5, CHECKSUM);
            ps.setString(6, container);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getObject("id", UUID.class);
            }
        }
    }

    private List<String> tableShape(String table) throws SQLException {
        return column("""
                select column_name || ' ' || data_type || coalesce('(' || character_maximum_length || ')', '')
                       || ' ' || is_nullable
                from information_schema.columns
                where table_schema = 'public' and table_name = ?
                order by ordinal_position
                """, table);
    }

    private String scalar(String sql, Object... args) throws SQLException {
        List<String> values = column(sql, args);
        return values.isEmpty() ? null : values.get(0);
    }

    private List<String> column(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (rs.next()) {
                    values.add(rs.getString(1));
                }
                return values;
            }
        }
    }

    /** @return null, если строка прошла ограничения; иначе SQLSTATE нарушенного ограничения. */
    private String sqlStateOf(String sql, Object... args) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, args);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return e.getSQLState();
        }
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }
}
