package com.evosome.poseloombackend.uploading.repository;

import com.evosome.poseloombackend.uploading.model.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
}
