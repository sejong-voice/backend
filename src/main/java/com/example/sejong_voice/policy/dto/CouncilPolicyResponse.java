package com.example.sejong_voice.policy.dto;

import com.example.sejong_voice.council.entity.Council;
import java.math.BigDecimal;

public record CouncilPolicyResponse(
    Integer minVote,
    BigDecimal approveRatioThreshold
) {
  public static CouncilPolicyResponse from(Council council) {
    return new CouncilPolicyResponse(
        council.getMinVote(),
        council.getApproveRatioThreshold()
    );
  }
}
