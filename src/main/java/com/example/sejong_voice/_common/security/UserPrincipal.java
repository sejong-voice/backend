package com.example.sejong_voice._common.security;

import com.example.sejong_voice.user.entity.UserRole;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UserPrincipal {
  private final UUID userId;
  private final String studentNo;
  private final UserRole role;
}
