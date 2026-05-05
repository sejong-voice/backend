package com.example.sejong_voice.report.dto;

import com.example.sejong_voice.report.entity.CommentReportReason;
import jakarta.validation.constraints.NotNull;

public record CommentReportRequest(@NotNull CommentReportReason reason) {}
