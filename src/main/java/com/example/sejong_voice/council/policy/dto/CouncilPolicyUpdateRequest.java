package com.example.sejong_voice.council.policy.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CouncilPolicyUpdateRequest(
    @NotNull @Min(0) Integer minVote,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal approveRatioThreshold) {}
