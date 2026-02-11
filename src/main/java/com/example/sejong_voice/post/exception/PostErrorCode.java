package com.example.sejong_voice.post.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

  POST_NOT_FOUND(404, "POST_001", "해당 게시글을 찾을 수 없습니다."),
  ACCESS_DENIED(403, "POST_002", "해당 게시글에 대한 접근이 거부되었습니다.");

  private final int status;
  private final String code;
  private final String message;
}