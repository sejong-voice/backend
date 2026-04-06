package com.example.sejong_voice.vote.dto;

import com.example.sejong_voice.vote.entity.Vote;
import com.example.sejong_voice.vote.entity.VoteChoice;
import java.time.Instant;
import java.util.UUID;

public record VoteParticipantResponse(
    UUID userId,
    String studentNo,
    String name,
    String department,
    VoteChoice choice,
    Instant votedAt
) {

  public static VoteParticipantResponse from(Vote vote) {
    Instant votedAt = vote.getUpdatedAt() != null ? vote.getUpdatedAt() : vote.getCreatedAt();

    return new VoteParticipantResponse(
        vote.getUser().getId(),
        vote.getUser().getStudentNo(),
        vote.getUser().getName(),
        vote.getUser().getDepartment(),
        vote.getChoice(),
        votedAt
    );
  }
}
