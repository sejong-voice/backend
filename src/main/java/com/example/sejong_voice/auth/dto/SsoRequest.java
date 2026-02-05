package com.example.sejong_voice.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SsoRequest(
        // 사실 front에서 받는 LoginRequestDto랑 데이터셋이 동일해서 LoginRequestDto 써도 되긴한데,
        // 일단 분리해둠
        @JsonProperty("student_id") @NotBlank String studentNo,
        @NotBlank String password
) { }
