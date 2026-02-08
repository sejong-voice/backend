package com.example.sejong_voice.auth.dto;

public record LoginResponse(
        String studentNo,
        String accessToken,
        long expiresIn
) {
  public static LoginResponse of(String studentNo, String accessToken, long expiresIn) {
    return new LoginResponse(studentNo, accessToken, expiresIn);
  }
}
