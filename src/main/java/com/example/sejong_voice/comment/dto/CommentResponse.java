package com.example.sejong_voice.comment.dto;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    String userName,
    String content,
    CommentStatus status,
    boolean canDelete,
    List<CommentResponse> children,
    Instant createdAt
) {

  public static CommentResponse from(Comment comment, UUID currentUserId) {
    return new CommentResponse(
        comment.getId(),
        comment.getUser().getName(),
        comment.getStatus() == CommentStatus.ACTIVE ? comment.getContent() :
            (comment.getStatus() == CommentStatus.DELETED ? "삭제된 댓글입니다." : "숨김 처리된 댓글입니다."),
        comment.getStatus(),
        comment.getUser().getId().equals(currentUserId),
        comment.getChildren().stream()
            .filter(child -> child.getStatus()
                == CommentStatus.ACTIVE)// 조회 제약뿐만 아니라, 반환값 만들때도 2중으로 status제약
            .map(child -> CommentResponse.from(child, currentUserId))
            .toList()
        ,
        comment.getCreatedAt()
    );
  }
}
