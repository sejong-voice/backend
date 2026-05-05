package com.example.sejong_voice.vote.dto;

public record VoteSummaryResponse(long agreeCount, long disagreeCount, long totalCount) {

  public static VoteSummaryResponse of(long agree, long disagree) {
    return new VoteSummaryResponse(agree, disagree, agree + disagree);
  }
}
