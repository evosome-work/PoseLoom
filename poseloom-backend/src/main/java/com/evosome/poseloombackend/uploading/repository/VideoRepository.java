package com.evosome.poseloombackend.uploading.repository;

import com.evosome.poseloombackend.uploading.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {
}
