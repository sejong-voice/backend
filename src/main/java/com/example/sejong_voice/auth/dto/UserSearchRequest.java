package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UserSearchRequest(@NotBlank String studentNo // @NotBlank 적용 가능(학번 필수 검색이라면)
    ) {}
