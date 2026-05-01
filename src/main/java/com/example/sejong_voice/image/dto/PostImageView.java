package com.example.sejong_voice.image.dto;

import java.util.UUID;

public record PostImageView(
    UUID imageId,
    String imageUrl
) {
  public static PostImageView of(UUID imageId, String imageUrl) {
    return new PostImageView(imageId, imageUrl);
  }
}
