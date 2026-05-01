package com.example.sejong_voice.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

// application.properties에서 자동으로 특정 키들을 자동으로 매핑해옴. jwtutil에서 사용함
@ConfigurationProperties(prefix = "jwt")
@Getter
@RequiredArgsConstructor
public class JwtProperties {
  private final String issuer;
  private final String secretKey;
  private final Long accessTokenExpiration;
  private final Long refreshTokenExpiration;
}
