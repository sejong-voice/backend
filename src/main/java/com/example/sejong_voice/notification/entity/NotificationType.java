package com.example.sejong_voice.notification.entity;

public enum NotificationType {
    POST_APPROVED,          // 게시글 승인
    POST_CREATED,           // 게시글 생성
    POST_EARLY_CLOSABLE,    // 조기 종료 가능 시점
    COMMENT_CREATED         // 댓글 생성
}
// 해당 컬럼들에 따라 알림을 보내주기 위함. 개시글 승인만 우선 할거지만 미리 정리해둔거
