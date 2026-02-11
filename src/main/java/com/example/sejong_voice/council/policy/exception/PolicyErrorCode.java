package com.example.sejong_voice.council.policy.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PolicyErrorCode implements ErrorCode {

  POLICY_NOT_FOUND(404, "POLICY_001", "정책을 찾을 수 없습니다.");

  private final int status;
  private final String code;
  private final String message;
}