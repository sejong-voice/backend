package com.example.sejong_voice.report.dto;

import com.example.sejong_voice.post.entity.PostModerationStatus;
import jakarta.validation.constraints.NotNull;

public record PostModerationRequest(
    @NotNull PostModerationStatus actionType
) {}
