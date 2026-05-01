package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record QuitRequest(@NotNull @AssertTrue(message = "탈퇴 동의가 필요합니다.") Boolean agreed) {}
