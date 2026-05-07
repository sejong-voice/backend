package com.example.sejong_voice.image.repository;

import com.example.sejong_voice.image.entity.PostStatementImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostStatementImageRepository extends JpaRepository<PostStatementImage, UUID> {

  List<PostStatementImage> findAllByPostStatementIdOrderByDisplayOrderAsc(UUID postStatementId);

  @Query(
      """
      SELECT psi
      FROM PostStatementImage psi
      JOIN FETCH psi.postStatement ps
      JOIN FETCH psi.image i
      WHERE ps.id IN :postStatementIds
        AND i.deletedAt IS NULL
      ORDER BY ps.id ASC, psi.displayOrder ASC
      """)
  List<PostStatementImage> findAllByPostStatementIdInOrderByPostStatementIdAscDisplayOrderAsc(
      @Param("postStatementIds") Collection<UUID> postStatementIds);

  List<PostStatementImage> findAllByPostStatementPostId(UUID postId);

  boolean existsByImageId(UUID imageId);

  void deleteAllByPostStatementPostId(UUID postId);
}
