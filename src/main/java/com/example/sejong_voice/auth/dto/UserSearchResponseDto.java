package com.example.sejong_voice.auth.dto;

import com.example.sejong_voice.auth.User;
import com.example.sejong_voice.auth.UserRole;
import com.example.sejong_voice.auth.UserStatus;

import java.util.UUID;

public record UserSearchResponseDto(
        UUID id,
        String studentId,
        String name,
        String department,
        UserRole role,
        UserStatus status
) {
    public static UserSearchResponseDto from(User user){
        return new UserSearchResponseDto(
                user.getId(),
                user.getStudentNo(),
                user.getName(),
                user.getDepartment(),
                user.getRole(),
                user.getStatus()
        );
    }
}
