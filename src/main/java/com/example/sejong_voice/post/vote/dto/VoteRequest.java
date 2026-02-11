package com.example.sejong_voice.post.vote.dto;

import com.example.sejong_voice.post.vote.entity.VoteChoice;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
    @NotNull VoteChoice choice
) {}