package com.evosome.poseloombackend.uploading.repository;

import com.evosome.poseloombackend.uploading.model.UploadSession;
import com.evosome.poseloombackend.uploading.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий {@link Video} (порт персистентности модуля загрузки, T2.2).
 *
 * <p>В эпике 001 ролик только создаётся по завершении сессии и читается по ней же:
 * каталог владельца, медиаданные и событие {@code video.ready} — эпик 002, поэтому
 * пагинации и проекций здесь нет.
 */
public interface VideoRepository extends JpaRepository<Video, UUID> {

    /**
     * Ролик, созданный этой сессией загрузки (связь 1:0..1, {@code uk_videos_upload_session}).
     *
     * <p>Нужен для идемпотентного {@code complete}: повторный вызов уже завершённой сессии
     * возвращает {@code 200} с той же {@link Video}, а не дубль (Story 1 AC3, T3.5).
     */
    Optional<Video> findByUploadSessionId(UUID uploadSessionId);

    /**
     * Признак того, что ролик по сессии уже существует — дешёвая проверка перед веткой
     * «повторный complete» без загрузки строки.
     */
    boolean existsByUploadSessionId(UUID uploadSessionId);

    /**
     * Тот же ролик, но с загрузкой {@link UploadSession} не требуется: идентификатор сессии
     * известен вызывающему. Оставлено как единственный способ отличить «дубль при параллельном
     * complete» от «сессия ещё не завершена» в тесте unique-constraint (DoD T2.2).
     */
    default Optional<Video> findBySession(UUID uploadSessionId) {
        return findByUploadSessionId(uploadSessionId);
    }
}
