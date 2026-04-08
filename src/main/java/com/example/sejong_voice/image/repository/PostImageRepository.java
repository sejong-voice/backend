package com.example.sejong_voice.image.repository;

import com.example.sejong_voice.image.entity.PostImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

  List<PostImage> findAllByPostIdOrderByDisplayOrderAsc(UUID postId);

  List<PostImage> findAllByPostIdInOrderByDisplayOrderAsc(Collection<UUID> postIds);

  void deleteAllByPostId(UUID postId);
}
