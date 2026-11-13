package com.evosome.poseloombackend.uploading.repository;

import com.evosome.poseloombackend.uploading.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    /**
     * Video по сессии: связь 1:1 ({@code uk_videos_upload_session}); нужна для
     * повторного complete — {@code 200} с той же Video (Tasks T3.5).
     */
    Optional<Video> findByUploadSessionId(UUID uploadSessionId);
}
