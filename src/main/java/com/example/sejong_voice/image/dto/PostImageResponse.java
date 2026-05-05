package com.example.sejong_voice.image.dto;

import java.util.UUID;

public record PostImageResponse(UUID imageId, String imageUrl) {
  public static PostImageResponse of(UUID imageId, String imageUrl) {
    return new PostImageResponse(imageId, imageUrl);
  }
}
