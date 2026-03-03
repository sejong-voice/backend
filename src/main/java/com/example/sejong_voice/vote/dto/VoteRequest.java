package com.example.sejong_voice.vote.dto;

import com.example.sejong_voice.vote.entity.VoteChoice;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
    @NotNull VoteChoice choice
) {}