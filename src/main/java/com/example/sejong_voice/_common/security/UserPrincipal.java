package com.example.sejong_voice._common.security;

import com.example.sejong_voice.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
  private UUID userId;
  private String studentNo;
  private UserRole role;
}