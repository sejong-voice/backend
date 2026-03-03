package com.example.sejong_voice.policy.dto;

import com.example.sejong_voice.policy.entity.CouncilPolicy;
import java.math.BigDecimal;

public record CouncilPolicyResponse(
    Integer minVote,
    BigDecimal approveRatioThreshold
) {
  public static CouncilPolicyResponse from(CouncilPolicy policy) {
    return new CouncilPolicyResponse(
        policy.getMinVote(),
        policy.getApproveRatioThreshold()
    );
  }
}