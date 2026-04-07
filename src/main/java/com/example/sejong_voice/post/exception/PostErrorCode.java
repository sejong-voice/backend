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
  RESULT_UPDATE_NOT_ALLOWED(400, "POST_005", "APPROVED, COMPLETED, REJECTED 상태의 게시글만 결과문을 등록하거나 수정할 수 있습니다.");

  private final int status;
  private final String code;
  private final String message;
}
