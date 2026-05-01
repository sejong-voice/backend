package com.example.sejong_voice.post.dto;

import com.example.sejong_voice.post.entity.PostStatus;
import java.util.UUID;

public record PostSearchCondition(
    PostStatus status,
    UUID councilId,
    UUID categoryId,
    String keyword,
    Boolean mine,
    Boolean assignedToMe,
    UUID currentUserId) {

  public PostSearchCondition withCurrentUserId(UUID currentUserId) {
    return new PostSearchCondition(
        status, councilId, categoryId, keyword, mine, assignedToMe, currentUserId);
  }
}
