package com.example.sejong_voice._common;

import com.example.sejong_voice.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
  private final LocalDateTime timestamp;
  private final int status;
  private final String error;
  private final String code;
  private final String message;

  public static ErrorResponse of(ErrorCode errorCode) {
    return ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(errorCode.getStatus())
        .error(errorCode.getClass().getSimpleName())
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .build();
  }
}