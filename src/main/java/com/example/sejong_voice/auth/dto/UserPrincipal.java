package com.example.sejong_voice.auth.dto;

import com.example.sejong_voice.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    @NotBlank String userId;
    @NotBlank String studentNo;
    @NotNull UserRole role;
}
