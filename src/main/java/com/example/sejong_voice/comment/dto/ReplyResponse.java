package com.example.sejong_voice.comment.dto;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ReplyResponse(
    UUID id,
    Integer anonymousNumber,
    boolean postAuthor,
    String content,
    CommentStatus status,
    boolean canDelete,
    Instant createdAt
) {

  public static ReplyResponse from(
      Comment comment,
      UUID currentUserId,
      UUID postAuthorId,
      Map<UUID, Integer> anonymousNumberByUserId
  ) {
    return new ReplyResponse(
        comment.getId(),
        CommentResponse.resolveAnonymousNumber(comment, anonymousNumberByUserId),
        CommentResponse.isPostAuthor(comment, postAuthorId),
        CommentResponse.resolveDisplayContent(comment),
        comment.getStatus(),
        CommentResponse.canDelete(comment, currentUserId),
        comment.getCreatedAt()
    );
  }
}
