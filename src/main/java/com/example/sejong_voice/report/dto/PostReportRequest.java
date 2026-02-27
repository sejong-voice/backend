package com.example.sejong_voice.report.dto;

import com.example.sejong_voice.report.entity.PostReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostReportRequest(
    @NotNull PostReportReason reason,
    @Size(max = 1000) String description
) {}
