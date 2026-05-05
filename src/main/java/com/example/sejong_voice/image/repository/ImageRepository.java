package com.example.sejong_voice.image.repository;

import com.example.sejong_voice.image.entity.Image;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, UUID> {

  List<Image> findAllByIdIn(Collection<UUID> ids);

  @Query(
      """
      select i
      from Image i
      where i.deletedAt is null
        and i.createdAt < :createdAt
        and not exists (
          select 1
          from PostImage pi
          where pi.image = i
        )
        and not exists (
          select 1
          from PostStatementImage psi
          where psi.image = i
        )
  """)
  List<Image> findAllExpiredTempImagesBefore(@Param("createdAt") Instant createdAt);
}
