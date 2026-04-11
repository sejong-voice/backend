package com.example.sejong_voice.comment.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

  COMMENT_NOT_FOUND(404, "COMMENT_001", "해당 댓글을 찾을 수 없습니다."),
  ACCESS_DENIED(403, "COMMENT_002", "해당 댓글에 대한 접근이 거부되었습니다."),
  ROOT_COMMENT_REQUIRED(400, "COMMENT_003", "대댓글은 루트 댓글에만 작성할 수 있습니다."),
  ROOT_COMMENT_POST_MISMATCH(400, "COMMENT_004", "루트 댓글이 요청한 게시글에 속하지 않습니다."),
  ROOT_COMMENT_NOT_ACTIVE(400, "COMMENT_005", "삭제되거나 숨김 처리된 댓글에는 대댓글을 작성할 수 없습니다.");

  private final int status;
  private final String code;
  private final String message;
}
