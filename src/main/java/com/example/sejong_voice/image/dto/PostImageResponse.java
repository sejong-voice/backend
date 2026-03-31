package com.example.sejong_voice.image.dto;

import java.util.UUID;

public record PostImageResponse(
    UUID imageId,
    String imageUrl
) {
}
