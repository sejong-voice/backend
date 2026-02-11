package com.example.sejong_voice.post.dto;

import jakarta.validation.constraints.NotBlank;

public record PostUpdateRequest(
    @NotBlank String title,
    @NotBlank String content
) {

}