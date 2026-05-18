package com.example.sejong_voice.vote.dto;

import java.math.BigDecimal;

public record VoteSummaryResponse(
    long agreeCount,
    long disagreeCount,
    long totalCount,
    int minVote,
    long remainingVotesToMinVote,
    BigDecimal requiredAgreeRatio,
    BigDecimal agreeRatio) {

  public static VoteSummaryResponse of(
      long agree,
      long disagree,
      int minVote,
      BigDecimal requiredAgreeRatio,
      BigDecimal agreeRatio) {
    long totalCount = agree + disagree;
    long remainingVotesToMinVote = Math.max(0L, (long) minVote - totalCount);
    return new VoteSummaryResponse(
        agree,
        disagree,
        totalCount,
        minVote,
        remainingVotesToMinVote,
        requiredAgreeRatio,
        agreeRatio);
  }
}
