package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PostDetailResponse(
    UUID id,
    UUID userId,
    String userStudentNo,
    UUID councilId,
    String councilName,
    String title,
    String content,
    PostStatus status,
    PostVotingDuration postVotingDuration,
    LocalDateTime createdAt,
    LocalDateTime votingEndAt
) {

  public static PostDetailResponse from(Post post) {
    return PostDetailResponse.builder()
        .id(post.getId())
        .userId(post.getUser().getId())
        .userStudentNo(post.getUser().getStudentNo())
        .councilId(post.getCouncil().getId())
        .councilName(post.getCouncil().getName())
        .title(post.getTitle())
        .content(post.getContent())
        .status(post.getStatus())
        .postVotingDuration(post.getVotingDuration())
        .createdAt(post.getCreatedAt())
        .votingEndAt(post.getVotingEndAt())
        .build();
  }
}
