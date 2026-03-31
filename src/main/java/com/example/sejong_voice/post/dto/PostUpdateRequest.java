package com.example.sejong_voice.post.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record PostUpdateRequest(
    @NotBlank String title,
    @NotBlank String content,
    @Nullable List<UUID> imageIds
) {

}
