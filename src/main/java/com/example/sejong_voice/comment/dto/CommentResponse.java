package com.example.sejong_voice.comment.dto;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    String userName,
    String content,
    CommentStatus status,
    List<CommentResponse> children,
    LocalDateTime createdAt
) {

  public static CommentResponse from(Comment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getUser().getName(),
        comment.getStatus() == CommentStatus.ACTIVE ? comment.getContent() :
            (comment.getStatus() == CommentStatus.DELETED ? "삭제된 댓글입니다." : "숨김 처리된 댓글입니다."),
        comment.getStatus(),
        comment.getChildren().stream().map(CommentResponse::from).toList(),
        comment.getCreatedAt()
    );
  }
}
