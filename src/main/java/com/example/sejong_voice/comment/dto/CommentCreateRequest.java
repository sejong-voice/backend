package com.example.sejong_voice.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CommentCreateRequest(
    @NotNull UUID postId,
    UUID rootCommentId,
    @NotBlank String content
) {

}
