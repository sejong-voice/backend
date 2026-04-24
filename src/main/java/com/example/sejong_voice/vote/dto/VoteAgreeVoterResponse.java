package com.example.sejong_voice.vote.dto;

import com.example.sejong_voice.vote.entity.Vote;
import java.time.Instant;

public record VoteAgreeVoterResponse(
    String studentNo,
    String department,
    Instant votedAt
) {

  public static VoteAgreeVoterResponse from(Vote vote) {
    Instant votedAt = vote.getUpdatedAt() != null ? vote.getUpdatedAt() : vote.getCreatedAt();

    return new VoteAgreeVoterResponse(
        vote.getUser().getStudentNo(),
        vote.getUser().getDepartment(),
        votedAt
    );
  }
}
