package com.example.sejong_voice.notification.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
  NOTIFICATION_NOT_FOUND(404, "NOTIFICATION_001", "해당 알림을 찾을 수 없습니다."),
  ACCESS_DENIED(403, "NOTIFICATION_002", "해당 알림에 대한 접근 권한이 없습니다.");

  private final int status;
  private final String code;
  private final String message;
}
