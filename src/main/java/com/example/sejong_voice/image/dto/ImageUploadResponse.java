package com.example.sejong_voice.image.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ImageUploadResponse(
    UUID imageId,
    String imageUrl
) {

  public static ImageUploadResponse of(UUID imageId, String imageUrl) {
    return ImageUploadResponse.builder()
        .imageId(imageId)
        .imageUrl(imageUrl)
        .build();
  }
}
