package com.example.sejong_voice.report.dto;

import com.example.sejong_voice.comment.entity.CommentStatus;
import jakarta.validation.constraints.NotNull;

public record CommentModerationRequest(@NotNull CommentStatus actionType) {}
