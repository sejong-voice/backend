package com.example.sejong_voice.auth.dto;

import com.example.sejong_voice.auth.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRequest {
    @NotNull UserRole role;
    @NotBlank String studentNo;
}
