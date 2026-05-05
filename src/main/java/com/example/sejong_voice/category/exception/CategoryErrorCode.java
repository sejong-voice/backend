package com.example.sejong_voice.category.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
  CATEGORY_NOT_FOUND(404, "CATEGORY_001", "해당 카테고리를 찾을 수 없습니다.");

  private final int status;
  private final String code;
  private final String message;
}
