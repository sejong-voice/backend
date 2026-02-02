package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto (
        @NotBlank String studentId,
        @NotBlank String password
){}
