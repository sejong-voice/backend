package com.example.sejong_voice.post.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostVotingDuration {
  ONE_WEEK("1주", 7),
  TWO_WEEK("2주", 14),
  FOUR_WEEK("4주(1개월)", 28);

  private final String description;
  private final int days;

  public LocalDateTime calculateDeadline(LocalDateTime startAt) {
    return startAt.plusDays(this.days);
  }
}