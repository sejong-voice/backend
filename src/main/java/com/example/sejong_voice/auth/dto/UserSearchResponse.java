package com.example.sejong_voice.auth.dto;

import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import java.util.UUID;

// 응답값은 notnull 설정을 해줘도 bean validation이
// 자동으로 돌지 않으니 따로 설정하지 않으면 큰 의미가 없어서 안함
public record UserSearchResponse(
    UUID id, String studentNo, String name, String department, UserRole role, UserStatus status) {
  public static UserSearchResponse from(User user) {
    return new UserSearchResponse(
        user.getId(),
        user.getStudentNo(),
        user.getName(),
        user.getDepartment(),
        user.getRole(),
        user.getStatus());
  }
}
