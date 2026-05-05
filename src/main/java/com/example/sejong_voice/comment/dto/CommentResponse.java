package com.example.sejong_voice.comment.dto;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.comment.exception.CommentErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.entity.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    String authorName,
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
        resolveAuthorName(comment, postAuthorId, anonymousNumberByUserId),
        resolveDisplayContent(comment),
        comment.getStatus(),
        isPostAuthor(comment, postAuthorId),
        canDelete(comment, currentUserId),
        comment.getCreatedAt(),
        replies);
  }

  static String resolveAuthorName(
      Comment comment, UUID postAuthorId, Map<UUID, Integer> anonymousNumberByUserId) {
    if (comment.getStatus() != CommentStatus.ACTIVE) {
      return switch (comment.getStatus()) {
        case DELETED -> "(삭제)";
        case BLINDED -> "(숨김)";
        default -> throw new BusinessException(CommentErrorCode.INVALID_COMMENT_STATUS);
      };
    }

    if (comment.getUser().getRole() == UserRole.ADMIN) {
      return comment.getUser().getAffiliation().getCouncilDisplayName();
    }

    if (isPostAuthor(comment, postAuthorId)) {
      return "익명(글쓴이)";
    }

    Integer anonymousNumber = anonymousNumberByUserId.get(comment.getUser().getId());
    if (anonymousNumber == null) {
      throw new BusinessException(CommentErrorCode.ANONYMOUS_NUMBER_NOT_FOUND);
    }

    return "익명" + anonymousNumber;
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
