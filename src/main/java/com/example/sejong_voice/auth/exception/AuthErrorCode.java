package com.example.sejong_voice.auth.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  INVALID_PASSWORD(401, "AUTH_001", "학번 또는 비밀번호가 일치하지 않습니다."),
  USER_SUSPENDED(403, "AUTH_002", "정지된 계정입니다."),
  USER_NOT_FOUND(404, "AUTH_003", "유저를 찾을 수 없습니다."),
  INVALID_TOKEN(401, "AUTH_004", "해당 인증 정보가 존재하지 않습니다. 다시 로그인해주세요."),
  TOKEN_EXPIRED(401, "AUTH_005", "만료된 세션입니다. 다시 로그인해주세요."),
  NOT_SUPER_ACCOUNT(403, "AUTH_006", "Super 계정만 접근 가능한 기능입니다."),
  SSO_AUTH_FAILED(403, "AUTH_007", "SSO 인증에 실패하였습니다."),
  INVALID_USER(401, "AUTH_008", "유효하지 않은 사용자입니다."),
  INVALID_AFFILIATION(403, "AUTH_009", "등록되지 않은 소속(학과)입니다."),
  NOT_AGREED(403, "AUTH_010", "서비스, 정책 이용 약관이 미동의 상태입니다."),
  QUIT_PENDING(403, "AUTH_011", "탈퇴 예정 계정입니다. 계정 삭제 전까지 이용할 수 없습니다.");

  private final int status;
  private final String code;
  private final String message;
}
