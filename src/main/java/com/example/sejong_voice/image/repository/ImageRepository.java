package com.example.sejong_voice.image.repository;

import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.entity.ImageStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, UUID> {

  List<Image> findAllByIdIn(Collection<UUID> ids);

  List<Image> findAllByStatusAndCreatedAtBefore(ImageStatus status, Instant createdAt);
}
