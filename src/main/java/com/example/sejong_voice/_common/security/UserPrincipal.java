package com.example.sejong_voice._common.security;

import com.example.sejong_voice.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class UserPrincipal {
  private final UUID userId;
  private final String studentNo;
  private final UserRole role;
}