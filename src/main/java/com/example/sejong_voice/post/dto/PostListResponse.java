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
public record PostListResponse(
    UUID id,
    UUID userId,
    String userStudentNo,
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
    Instant votingEndAt,
    List<PostImageResponse> images
) {
  private static final int MASK_SUFFIX_LENGTH = 3;

  public static PostListResponse from(Post post) {
    return from(post, 0L, 0L);
  }

  public static PostListResponse from(Post post, long voteCount, long commentCount) {
    return PostListResponse.builder()
        .id(post.getId())
        .userId(post.getUser().getId())
        .userStudentNo(maskStudentNo(post.getUser().getStudentNo()))
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
        .images(List.of())
        .build();
  }

  private static String maskStudentNo(String studentNo) {
    return studentNo.substring(0, studentNo.length() - MASK_SUFFIX_LENGTH) + "***";
  }

  public PostListResponse withCounts(long voteCount, long commentCount) {
    return PostListResponse.builder()
        .id(id)
        .userId(userId)
        .userStudentNo(userStudentNo)
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
        .images(images)
        .build();
  }

  public PostListResponse withImages(List<PostImageResponse> images) {
    return PostListResponse.builder()
        .id(id)
        .userId(userId)
        .userStudentNo(userStudentNo)
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
        .images(images)
        .build();
  }
}
