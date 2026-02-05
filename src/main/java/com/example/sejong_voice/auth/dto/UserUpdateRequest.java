package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String studentNo, // @NotBlank 적용 가능(전체 업데이트 정책이면)
        @NotBlank String name       // @NotBlank 적용 가능(이름 필수로 받을 때)
) {}

//NotBlank : 공란 뿐 아니라 빈 문자열도 허용하지 않음.
