package com.example.sejong_voice.auth.dto;

import com.example.sejong_voice.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RoleUpdateRequest {
  @NotNull UserRole role;
}
