package com.example.sejong_voice.post.repository;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID>, PostRepositoryCustom {

  Optional<Post> findByIdAndDeletedAtIsNull(UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Post p where p.id = :id and p.deletedAt is null")
  Optional<Post> findByIdForUpdate(@Param("id") UUID id);

  List<Post> findAllByStatusAndVotingEndAtBefore(PostStatus status, Instant now);
}
