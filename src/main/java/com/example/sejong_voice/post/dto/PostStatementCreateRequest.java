package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.post.entity.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PostStatementCreateRequest(
    PostStatus finalStatus,
    @NotBlank String content,
    @NotNull List<@NotNull UUID> imageIds
) {
}
