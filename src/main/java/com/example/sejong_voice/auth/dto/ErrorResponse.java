package com.example.sejong_voice.auth.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code, // 서비스 전역 에러 코드
        String message,
        String path
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        //ErrorResponse의 값을 한 번에 모두 꺼내기 위함
        return new ErrorResponse(
                LocalDateTime.now(),
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                code,
                message,
                path
        );
    }
}
