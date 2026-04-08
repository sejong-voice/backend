package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.post.entity.PostStatement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PostStatementResponse(
    UUID id,
    Integer sequence,
    String content,
    Instant createdAt,
    List<PostImageResponse> images
) {

  public static PostStatementResponse from(PostStatement postStatement) {
    return PostStatementResponse.builder()
        .id(postStatement.getId())
        .sequence(postStatement.getSequence())
        .content(postStatement.getContent())
        .createdAt(postStatement.getCreatedAt())
        .images(List.of())
        .build();
  }

  public PostStatementResponse withImages(List<PostImageResponse> images) {
    return new PostStatementResponse(id, sequence, content, createdAt, images);
  }
}
