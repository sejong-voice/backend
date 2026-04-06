package com.example.sejong_voice.vote.dto;

import java.math.BigDecimal;
import java.util.List;

public record VoteDetailResponse(
    long agreeCount,
    long disagreeCount,
    long totalCount,
    BigDecimal agreeRatio,
    List<VoteDepartmentDistributionResponse> departmentDistributions,
    List<VoteParticipantResponse> participants
) {

  public static VoteDetailResponse of(
      long agreeCount,
      long disagreeCount,
      long totalCount,
      BigDecimal agreeRatio,
      List<VoteDepartmentDistributionResponse> departmentDistributions,
      List<VoteParticipantResponse> participants
  ) {
    return new VoteDetailResponse(
        agreeCount,
        disagreeCount,
        totalCount,
        agreeRatio,
        departmentDistributions,
        participants
    );
  }
}
