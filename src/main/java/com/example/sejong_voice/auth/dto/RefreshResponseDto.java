package com.example.sejong_voice.auth.dto;

public record RefreshResponseDto(
        String studentId,
        String accessToken,
        long expiresIn
) {}
// 학번, 토큰, 만료기한 반환