package com.example.sejong_voice.auth.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SsoResponse(
        SsoUser user,          // 유저 정보 객체 // @NotNull 적용 가능(SsoUser 필수일 때)
        SsoAuthResult sso      // SSO 인증 결과 객체 (선택적)
) {}
