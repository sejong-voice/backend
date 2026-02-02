package com.example.sejong_voice.auth.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String code, // 서비스 전역 에러 코드
        String message,
        String path
) {
    public static ErrorResponseDto of(int status, String code, String message, String path) {
        return new ErrorResponseDto(
                LocalDateTime.now(),
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                code,
                message,
                path
        );
    }
}
