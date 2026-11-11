package com.evosome.poseloombackend.uploading.repository;

import com.evosome.poseloombackend.uploading.model.UploadSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий {@link UploadSession} (порт персистентности модуля загрузки, T2.2).
 *
 * <p>Методы — только под операции эпика 001 ({@code create} / {@code presign part} /
 * {@code complete}); спекулятивных запросов нет: {@code GET}-статус и прогресс — эпик 002,
 * TTL-выборка брошенных сессий — отдельный эпик, {@code upload_parts} как таблица не вводится
 * (части всегда берутся из {@code ListParts}).
 *
 * <p>Запросы по владельцу опираются на индекс {@code idx_upload_sessions_uploader_status}.
 */
public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    /**
     * Сессия, принадлежащая конкретному инициатору.
     *
     * <p>Проверка владельца вшита в запрос, а не в ветку после {@code findById}: чужая и
     * несуществующая сессия дают одинаковый пустой {@code Optional}, поэтому контроллер
     * возвращает {@code 404 SESSION_NOT_FOUND} без раскрытия существования чужой записи
     * (Story 6 AC1, T3.2).
     */
    Optional<UploadSession> findByIdAndUploaderId(UUID id, UUID uploaderId);

    /**
     * Сессия с row-level блокировкой {@code SELECT ... FOR UPDATE}.
     *
     * <p>{@code complete} — единственная операция, меняющая {@code status} и {@code failure_code}
     * (bulk-UPDATE по ним запрещён, ER Deviations). Пессимистичная блокировка сериализует
     * параллельные {@code complete} одной сессии: второй транзакции не даёт создать дубль
     * {@link com.evosome.poseloombackend.uploading.model.Video}, а UNIQUE
     * {@code uk_videos_upload_session} остаётся финальной защитой (T3.5).
     *
     * <p>Выбор {@code FOR UPDATE} вместо {@code @Version} обусловлен схемой: колонки {@code version}
     * в {@code V0.1-alpha} нет, оптимистическая блокировка потребовала бы отдельной миграции.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UploadSession s where s.id = :id")
    Optional<UploadSession> findByIdForUpdate(@Param("id") UUID id);
}
