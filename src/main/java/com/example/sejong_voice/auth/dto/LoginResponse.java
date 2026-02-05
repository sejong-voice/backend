package com.example.sejong_voice.auth.dto;

public record LoginResponse(
        String studentNo,
        String accessToken,
        long expiresIn
) {}

//학번, accessToken, 만료기한(refreshToken 2주로 시작)
