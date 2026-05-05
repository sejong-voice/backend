package com.example.sejong_voice.council.dto;

import com.example.sejong_voice.council.entity.Council;
import java.util.UUID;

public record CouncilResponse(UUID id, String name) {
  public CouncilResponse(Council council) {
    this(council.getId(), council.getName());
  }
}
