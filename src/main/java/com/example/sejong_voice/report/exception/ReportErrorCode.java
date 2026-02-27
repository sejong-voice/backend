package com.example.sejong_voice.report.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {
    POST_NOT_FOUND(404, "REPORT_001", "신고 대상 게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(404, "REPORT_002", "신고 대상 댓글을 찾을 수 없습니다."),
    REPORT_NOT_FOUND(404, "REPORT_003", "신고 내역을 찾을 수 없습니다."),

    DUPLICATE_POST_REPORT(409, "REPORT_004", "이미 해당 게시글을 신고했습니다."),
    DUPLICATE_COMMENT_REPORT(409, "REPORT_005", "이미 해당 댓글을 신고했습니다."),
    SELF_REPORT_NOT_ALLOWED(400, "REPORT_006", "본인 작성글/댓글은 신고할 수 없습니다."),

    ADMIN_ONLY_ACTION(403, "REPORT_007", "관리자만 수행할 수 있는 작업입니다.");

    private final int status;
    private final String code;
    private final String message;
}
