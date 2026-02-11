package com.example.sejong_voice.council.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouncilErrorCode implements ErrorCode {

  COUNCIL_NOT_FOUND(404, "COUNCIL_001", "해당 게시글을 찾을 수 없습니다."),
  ACCESS_DENIED(403, "POST_002", "해당 게시글에 대한 접근이 거부되었습니다.");

  private final int status;
  private final String code;
  private final String message;
}