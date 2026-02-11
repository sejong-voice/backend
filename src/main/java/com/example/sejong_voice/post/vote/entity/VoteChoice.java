package com.example.sejong_voice.post.vote.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum VoteChoice {
  DISAGREE(0), // 반대
  AGREE(1);    // 찬성

  private final int value;
}
