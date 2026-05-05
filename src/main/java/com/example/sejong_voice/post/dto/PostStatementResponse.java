package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.image.dto.PostImageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PostStatementResponse(
    UUID id, Integer sequence, String content, Instant createdAt, List<PostImageResponse> images) {

  public static PostStatementResponse from(
      PostStatementView postStatement, List<PostImageResponse> images) {
    return PostStatementResponse.builder()
        .id(postStatement.id())
        .sequence(postStatement.sequence())
        .content(postStatement.content())
        .createdAt(postStatement.createdAt())
        .images(images)
        .build();
  }
}
