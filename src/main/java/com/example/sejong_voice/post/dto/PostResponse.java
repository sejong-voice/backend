package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PostResponse(
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
    LocalDateTime votingEndAt,
    List<PostImageResponse> images
) {

  public static PostResponse from(Post post) {
    return PostResponse.builder()
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
        .images(List.of())
        .build();
  }

  public PostResponse withImages(List<PostImageResponse> images) {
    return new PostResponse(
        id,
        userId,
        userStudentNo,
        councilId,
        councilName,
        title,
        content,
        status,
        postVotingDuration,
        createdAt,
        votingEndAt,
        images
    );
  }
}
