package com.example.sejong_voice.image.repository;

import com.example.sejong_voice.image.entity.PostStatementImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostStatementImageRepository extends JpaRepository<PostStatementImage, UUID> {

  List<PostStatementImage> findAllByPostStatementIdOrderByDisplayOrderAsc(UUID postStatementId);

  List<PostStatementImage> findAllByPostStatementIdInOrderByPostStatementIdAscDisplayOrderAsc(
      Collection<UUID> postStatementIds
  );

  List<PostStatementImage> findAllByPostStatementPostId(UUID postId);

  boolean existsByImageId(UUID imageId);

  void deleteAllByPostStatementPostId(UUID postId);
}
