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
    UUID categoryId,
    String categoryName,
    String title,
    String content,
    PostStatus status,
    PostVotingDuration postVotingDuration,
    boolean canCloseEarly,
    Instant createdAt,
    Instant votingEndAt,
    List<PostImageResponse> images,
    List<PostStatementResponse> statements
) {
  private static final int MASK_SUFFIX_LENGTH = 3;

  public static PostDetailResponse from(Post post) {
    return PostDetailResponse.builder()
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
        .canCloseEarly(false)
        .createdAt(post.getCreatedAt())
        .votingEndAt(post.getVotingEndAt())
        .images(List.of())
        .statements(List.of())
        .build();
  }

  private static String maskStudentNo(String studentNo) {
    return studentNo.substring(0, studentNo.length() - MASK_SUFFIX_LENGTH) + "***";
  }

  public PostDetailResponse withImages(List<PostImageResponse> images) {
    return new PostDetailResponse(
        id,
        userId,
        userStudentNo,
        councilId,
        councilName,
        categoryId,
        categoryName,
        title,
        content,
        status,
        postVotingDuration,
        canCloseEarly,
        createdAt,
        votingEndAt,
        images,
        statements
    );
  }

  public PostDetailResponse withStatements(List<PostStatementResponse> statements) {
    return new PostDetailResponse(
        id,
        userId,
        userStudentNo,
        councilId,
        councilName,
        categoryId,
        categoryName,
        title,
        content,
        status,
        postVotingDuration,
        canCloseEarly,
        createdAt,
        votingEndAt,
        images,
        statements
    );
  }

  public PostDetailResponse withCanCloseEarly(boolean canCloseEarly) {
    return new PostDetailResponse(
        id,
        userId,
        userStudentNo,
        councilId,
        councilName,
        categoryId,
        categoryName,
        title,
        content,
        status,
        postVotingDuration,
        canCloseEarly,
        createdAt,
        votingEndAt,
        images,
        statements
    );
  }
}
