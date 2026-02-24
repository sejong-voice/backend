package com.example.sejong_voice.comment.repository;

import com.example.sejong_voice.comment.entity.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
  @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
  List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") UUID postId);
}
