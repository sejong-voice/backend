package com.example.sejong_voice.comment.repository;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.post.dto.PostCountRow;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
//  @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
//  List<Comment> findAllByPostIdWithUserOrderByCreatedAtAsc(@Param("postId") UUID postId);
  @Query("""
  SELECT c
  FROM Comment c
  LEFT JOIN FETCH c.user
  WHERE c.post.id = :postId
    AND c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
  ORDER BY c.createdAt ASC
  """)
  List<Comment> findAllActiveByPostIdWithUserOrderByCreatedAtAsc(@Param("postId") UUID postId);

  @Query("""
      SELECT new com.example.sejong_voice.post.dto.PostCountRow(c.post.id, COUNT(c))
      FROM Comment c
      WHERE c.post.id IN :postIds
        AND c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
      GROUP BY c.post.id
      """)
  List<PostCountRow> countActiveByPostIds(@Param("postIds") Collection<UUID> postIds);

}
