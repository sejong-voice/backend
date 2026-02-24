package com.example.sejong_voice.comment.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

  COMMENT_NOT_FOUND(404, "COMMENT_001", "해당 댓글을 찾을 수 없습니다."),
  USER_NOT_FOUND(404, "COMMENT_002", "해당 작성자를 찾을 수 없습니다."),
  POST_NOT_FOUND(404, "COMMENT_003", "해당 게시글을 찾을 수 없습니다."),
  ACCESS_DENIED(403, "COMMENT_004", "해당 댓글에 대한 접근이 거부되었습니다.");

  private final int status;
  private final String code;
  private final String message;
}
