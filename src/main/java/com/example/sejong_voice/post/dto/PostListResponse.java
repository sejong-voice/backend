package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PostListResponse(
    UUID id,
    UUID userId,
    UUID councilId,
    String councilName,
    UUID categoryId,
    String categoryName,
    String title,
    String content,
    PostStatus status,
    PostVotingDuration postVotingDuration,
    Long voteCount,
    Long commentCount,
    Instant createdAt,
    Instant votingEndAt
) {
  public static PostListResponse from(Post post) {
    return from(post, 0L, 0L);
  }

  public static PostListResponse from(Post post, long voteCount, long commentCount) {
    return PostListResponse.builder()
        .id(post.getId())
        .userId(post.getUser().getId())
        .councilId(post.getCouncil().getId())
        .councilName(post.getCouncil().getName())
        .categoryId(post.getCategory().getId())
        .categoryName(post.getCategory().getName())
        .title(post.getTitle())
        .content(post.getContent())
        .status(post.getStatus())
        .postVotingDuration(post.getVotingDuration())
        .voteCount(voteCount)
        .commentCount(commentCount)
        .createdAt(post.getCreatedAt())
        .votingEndAt(post.getVotingEndAt())
        .build();
  }

  public PostListResponse withCounts(long voteCount, long commentCount) {
    return PostListResponse.builder()
        .id(id)
        .userId(userId)
        .councilId(councilId)
        .councilName(councilName)
        .categoryId(categoryId)
        .categoryName(categoryName)
        .title(title)
        .content(content)
        .status(status)
        .postVotingDuration(postVotingDuration)
        .voteCount(voteCount)
        .commentCount(commentCount)
        .createdAt(createdAt)
        .votingEndAt(votingEndAt)
        .build();
  }
}
