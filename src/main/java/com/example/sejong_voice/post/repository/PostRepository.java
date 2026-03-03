package com.example.sejong_voice.post.repository;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID>, PostRepositoryCustom {

  Optional<Post> findByIdAndDeletedAtIsNull(UUID id);

  @Query("select p from Post p join fetch p.user where p.id = :id and p.deletedAt is null")
  Optional<Post> findByIdWithUser(@Param("id") UUID id);

  List<Post> findAllByStatusAndVotingEndAtBefore(PostStatus status, LocalDateTime now);
}