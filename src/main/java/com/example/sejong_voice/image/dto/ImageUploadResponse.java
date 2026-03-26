package com.example.sejong_voice.image.dto;

import java.util.UUID;

public record ImageUploadResponse(
    UUID imageId,
    String imageUrl
) {
}
