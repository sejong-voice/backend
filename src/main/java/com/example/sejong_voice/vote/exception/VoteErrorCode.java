package com.example.sejong_voice.vote.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VoteErrorCode implements ErrorCode {

  UNKNOWN_VOTE_VALUE(404, "VOTE_001", "알 수 없는 투표 결과입니다.");

  private final int status;
  private final String code;
  private final String message;
}