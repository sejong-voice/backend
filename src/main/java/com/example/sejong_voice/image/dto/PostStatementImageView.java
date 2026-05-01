package com.example.sejong_voice.image.dto;

import java.util.UUID;

public record PostStatementImageView(
    UUID imageId,
    String imageUrl
) {
  public static PostStatementImageView of(UUID imageId, String imageUrl) {
    return new PostStatementImageView(imageId, imageUrl);
  }
}
