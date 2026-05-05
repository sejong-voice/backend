package com.example.sejong_voice._common;

import com.example.sejong_voice.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
  private final Instant timestamp;
  private final int status;
  private final String error;
  private final String code;
  private final String message;

  public static ErrorResponse of(ErrorCode errorCode, Clock clock) {
    return ErrorResponse.builder()
        .timestamp(Instant.now(clock))
        .status(errorCode.getStatus())
        .error(errorCode.getClass().getSimpleName())
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .build();
  }
}
