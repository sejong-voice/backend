package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String studentNo
) {}


//학번으로 로그아웃 받아서 테이블 조회 후 refreshToken을 삭제(studentId로 refreshToken 조회 후 삭제)