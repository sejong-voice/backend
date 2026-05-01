package com.example.sejong_voice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode { // ErrorCode 구현 필수!
  INVALID_INPUT_VALUE(400, "COMMON_001", "입력값이 올바르지 않습니다."),
  JSON_PARSE_ERROR(400, "COMMON_002", "JSON 형식이 올바르지 않습니다."),

  METHOD_NOT_ALLOWED(405, "COMMON_003", "허용되지 않는 HTTP 메서드입니다."),

  UNKNOWN_ERROR(500, "COMMON_004", "알 수 없는 오류가 발생했습니다.");

  private final int status;
  private final String code;
  private final String message;
}
