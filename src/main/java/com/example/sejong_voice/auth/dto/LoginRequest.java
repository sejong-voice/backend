package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String studentNo,
        @NotBlank String password
){}
