package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.post.entity.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostResultRequest(
    @NotNull PostStatus status,
    @NotBlank String resultContent
) {
}
