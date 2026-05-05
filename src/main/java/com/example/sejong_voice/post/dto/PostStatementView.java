package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.post.entity.PostStatement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostStatementView(UUID id, Integer sequence, String content, Instant createdAt) {

  public static List<PostStatementView> from(List<PostStatement> postStatements) {
    return postStatements.stream().map(PostStatementView::from).toList();
  }

  public static PostStatementView from(PostStatement postStatement) {
    return new PostStatementView(
        postStatement.getId(),
        postStatement.getSequence(),
        postStatement.getContent(),
        postStatement.getCreatedAt());
  }
}
