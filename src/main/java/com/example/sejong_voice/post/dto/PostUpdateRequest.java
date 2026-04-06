package com.example.sejong_voice.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PostUpdateRequest(
    @NotBlank String title,
    @NotBlank String content,
    @NotNull List<@NotNull UUID> imageIds
) {

}
