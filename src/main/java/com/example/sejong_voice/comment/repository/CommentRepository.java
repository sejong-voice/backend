package com.example.sejong_voice.comment.repository;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.post.dto.PostCountRow;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  @Query(
      value =
          """
          SELECT c
          FROM Comment c
          LEFT JOIN FETCH c.user
          WHERE c.post.id = :postId
            AND c.rootComment IS NULL
            AND (
              c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
              OR EXISTS (
                SELECT 1
                FROM Comment r
                WHERE r.rootComment = c
                  AND r.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
              )
            )
          """,
      countQuery =
          """
          SELECT COUNT(c)
          FROM Comment c
          WHERE c.post.id = :postId
            AND c.rootComment IS NULL
            AND (
              c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
              OR EXISTS (
                SELECT 1
                FROM Comment r
                WHERE r.rootComment = c
                  AND r.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
              )
            )
          """)
  Page<Comment> findDisplayableRootCommentsByPostId(
      @Param("postId") UUID postId, Pageable pageable);

  @Query(
      """
      SELECT c
      FROM Comment c
      LEFT JOIN FETCH c.user
      LEFT JOIN FETCH c.rootComment
      WHERE c.rootComment.id IN :rootCommentIds
        AND c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
      ORDER BY c.createdAt ASC
      """)
  List<Comment> findActiveRepliesByRootCommentIds(
      @Param("rootCommentIds") Collection<UUID> rootCommentIds);

  @Query(
      """
      SELECT c.user.id
      FROM Comment c
      WHERE c.post.id = :postId
      GROUP BY c.user.id
      ORDER BY MIN(c.createdAt) ASC
      """)
  List<UUID> findCommentAuthorIdsByPostIdOrderByFirstCommentAt(@Param("postId") UUID postId);

  @Query(
      """
      SELECT c
      FROM Comment c
      WHERE c.post.id = :postId
        AND c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
      ORDER BY c.createdAt ASC
      """)
  List<Comment> findAllActiveByPostId(@Param("postId") UUID postId);

  @Query(
      """
      SELECT COUNT(c)
      FROM Comment c
      WHERE c.post.id = :postId
        AND c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
      """)
  long countActiveByPostId(@Param("postId") UUID postId);

  @Query(
      """
      SELECT new com.example.sejong_voice.post.dto.PostCountRow(c.post.id, COUNT(c))
      FROM Comment c
      WHERE c.post.id IN :postIds
        AND c.status = com.example.sejong_voice.comment.entity.CommentStatus.ACTIVE
      GROUP BY c.post.id
      """)
  List<PostCountRow> countActiveByPostIds(@Param("postIds") Collection<UUID> postIds);
}
