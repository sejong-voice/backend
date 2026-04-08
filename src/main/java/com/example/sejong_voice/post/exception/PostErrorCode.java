package com.example.sejong_voice.post.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

  POST_NOT_FOUND(404, "POST_001", "해당 게시글을 찾을 수 없습니다."),
  ACCESS_DENIED(403, "POST_002", "해당 게시글에 대한 접근이 거부되었습니다."),
  POST_CANNOT_DELETE_VOTING_ENDED(400, "POST_003", "투표가 종료된 게시글은 삭제할 수 없습니다."),
  VOTE_DETAILS_NOT_AVAILABLE(400, "POST_004", "APPROVED, COMPLETED, REJECTED 상태의 게시글만 상세 결과를 조회할 수 있습니다."),
  INVALID_FINAL_STATUS(400, "POST_005", "최종 상태는 COMPLETED 또는 REJECTED만 사용할 수 있습니다."),
  STATEMENT_LIMIT_EXCEEDED(400, "POST_006", "입장문은 게시글당 최대 2개까지 생성할 수 있습니다."),
  STATEMENT_CREATE_NOT_ALLOWED(400, "POST_007", "APPROVED, COMPLETED 또는 REJECTED 상태의 게시글만 입장문을 생성할 수 있습니다."),
  FINAL_STATUS_REQUIRED(400, "POST_008", "첫 입장문 생성 시 최종 상태는 필수입니다."),
  FINAL_STATUS_NOT_ALLOWED(400, "POST_009", "추가 입장문 생성 시 최종 상태는 지정할 수 없습니다.");

  private final int status;
  private final String code;
  private final String message;
}
