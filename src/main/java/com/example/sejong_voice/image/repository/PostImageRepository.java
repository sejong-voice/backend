package com.example.sejong_voice.image.repository;

import com.example.sejong_voice.image.entity.PostImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

  @Query(
      """
      select pi
      from PostImage pi
      join fetch pi.image i
      where pi.post.id = :postId
        and i.deletedAt is null
      order by pi.displayOrder asc
  """)
  List<PostImage> findAllByPostIdOrderByDisplayOrderAsc(@Param("postId") UUID postId);

  boolean existsByImageId(UUID imageId);

  void deleteAllByPostId(UUID postId);
}
