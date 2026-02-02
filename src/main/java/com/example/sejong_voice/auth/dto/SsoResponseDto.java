package com.example.sejong_voice.auth.dto;

public record SsoResponseDto(
        SsoUserDto user,          // 유저 정보 객체
        SsoAuthResultDto sso      // SSO 인증 결과 객체 (선택적)
) {}
