package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsentLoginRequest(
    @NotBlank String studentNo,
    @NotBlank String password,
    @NotNull Boolean serviceAgreed,
    @NotNull Boolean policyAgreed) {}
