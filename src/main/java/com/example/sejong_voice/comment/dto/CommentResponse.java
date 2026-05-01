package com.example.sejong_voice.comment.dto;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    Integer anonymousNumber,
    String content,
    CommentStatus status,
    boolean postAuthor,
    boolean canDelete,
    Instant createdAt,
    List<ReplyResponse> replies) {

  public static CommentResponse from(
      Comment comment,
      UUID currentUserId,
      UUID postAuthorId,
      Map<UUID, Integer> anonymousNumberByUserId,
      List<ReplyResponse> replies) {
    return new CommentResponse(
        comment.getId(),
        resolveAnonymousNumber(comment, anonymousNumberByUserId),
        resolveDisplayContent(comment),
        comment.getStatus(),
        isPostAuthor(comment, postAuthorId),
        canDelete(comment, currentUserId),
        comment.getCreatedAt(),
        replies);
  }

  static Integer resolveAnonymousNumber(
      Comment comment, Map<UUID, Integer> anonymousNumberByUserId) {
    if (comment.getStatus() != CommentStatus.ACTIVE) {
      return null;
    }

    return anonymousNumberByUserId.get(comment.getUser().getId());
  }

  static boolean isPostAuthor(Comment comment, UUID postAuthorId) {
    if (comment.getStatus() != CommentStatus.ACTIVE) {
      return false;
    }

    return comment.getUser().getId().equals(postAuthorId);
  }

  static boolean canDelete(Comment comment, UUID currentUserId) {
    if (comment.getStatus() != CommentStatus.ACTIVE) {
      return false;
    }

    return comment.getUser().getId().equals(currentUserId);
  }

  static String resolveDisplayContent(Comment comment) {
    return switch (comment.getStatus()) {
      case ACTIVE -> comment.getContent();
      case DELETED -> "삭제된 댓글입니다.";
      case BLINDED -> "숨김 처리된 댓글입니다.";
    };
  }
}
