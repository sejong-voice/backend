package com.example.sejong_voice.auth.dto;

import com.example.sejong_voice.auth.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusRequest {
    @NotNull UserStatus status;
    @NotNull LocalDateTime duration;
}
