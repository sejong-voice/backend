package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import java.time.Instant;
import java.util.List;
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
    String resultContent,
    Instant resultCreatedAt,
    Instant resultUpdatedAt,
    PostVotingDuration postVotingDuration,
    Instant createdAt,
    Instant votingEndAt,
    List<PostImageResponse> images
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
        .resultContent(post.getResultContent())
        .resultCreatedAt(post.getResultCreatedAt())
        .resultUpdatedAt(post.getResultUpdatedAt())
        .postVotingDuration(post.getVotingDuration())
        .createdAt(post.getCreatedAt())
        .votingEndAt(post.getVotingEndAt())
        .images(List.of())
        .build();
  }

  public PostDetailResponse withImages(List<PostImageResponse> images) {
    return new PostDetailResponse(
        id,
        userId,
        userStudentNo,
        councilId,
        councilName,
        title,
        content,
        status,
        resultContent,
        resultCreatedAt,
        resultUpdatedAt,
        postVotingDuration,
        createdAt,
        votingEndAt,
        images
    );
  }
}
