package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.post.entity.PostVotingDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PostCreateRequest(
    @NotBlank String title,
    @NotBlank String content,
    @NotNull UUID councilId,
    @NotNull UUID categoryId,
    @NotNull PostVotingDuration postVotingDuration,
    @NotNull List<@NotNull UUID> imageIds
) {

}
