package com.example.sejong_voice.vote.dto;

import java.math.BigDecimal;
import java.util.List;

public record VoteStatisticsResponse(
    long agreeCount,
    long disagreeCount,
    long totalCount,
    BigDecimal agreeRatio,
    List<VoteDepartmentDistributionResponse> departmentDistributions) {

  public static VoteStatisticsResponse of(
      long agreeCount,
      long disagreeCount,
      long totalCount,
      BigDecimal agreeRatio,
      List<VoteDepartmentDistributionResponse> departmentDistributions) {
    return new VoteStatisticsResponse(
        agreeCount, disagreeCount, totalCount, agreeRatio, departmentDistributions);
  }
}
