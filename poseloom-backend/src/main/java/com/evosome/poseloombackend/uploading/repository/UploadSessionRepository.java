package com.evosome.poseloombackend.uploading.repository;

import com.evosome.poseloombackend.uploading.model.UploadSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    /**
     * Чтение сессии с проверкой владельца: чужая отвечает тем же отсутствием,
     * что и несуществующая (спека SessionNotFound.yml — существование не раскрывается).
     */
    Optional<UploadSession> findByIdAndUploaderId(UUID id, UUID uploaderId);

    /**
     * Чтение под {@code SELECT ... FOR UPDATE} для complete (Tasks T3.5):
     * сериализует параллельные complete одной сессии — второй поток после
     * снятия блокировки видит {@code COMPLETED} и отдаёт {@code 200} с той же Video.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from UploadSession s
            where s.id = :sessionId and s.uploaderId = :uploaderId
            """)
    Optional<UploadSession> findByIdAndUploaderIdForUpdate(@Param("sessionId") UUID sessionId,
                                                           @Param("uploaderId") UUID uploaderId);
}
