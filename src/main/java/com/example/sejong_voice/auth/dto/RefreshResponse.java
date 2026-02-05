package com.example.sejong_voice.auth.dto;

public record RefreshResponse(
        String studentNo,
        String accessToken,
        long expiresIn
) {}
// 학번, 토큰, 만료기한 반환