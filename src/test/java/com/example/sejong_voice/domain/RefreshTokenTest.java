package com.example.sejong_voice.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.auth.RefreshToken;
import com.example.sejong_voice.user.entity.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {
  private User dummyUser() {
    Affiliation affiliation = Affiliation.builder().name("컴퓨터공학과").build();

    return User.builder()
        .studentNo("20201234")
        .name("홍길동")
        .affiliation(affiliation)
        .password("ENC_PW")
        .duration(User.activeDuration())
        .createdAt(Instant.parse("2026-04-19T00:00:00Z"))
        .build();
  }

  @Test
  void isExpired_만료시각이_현재보다_이전이면_true() {
    // accessToken이 만료된 상황, 시간대별 검증이 잘 되는지 확인하기 위함
    Instant now = Instant.parse("2026-04-19T00:00:00Z");
    RefreshToken refreshToken =
        RefreshToken.builder()
            .user(dummyUser())
            .token("t1")
            .expiryDate(now.minusSeconds(1))
            .build();

    assertTrue(refreshToken.isExpired(now));
  }

  @Test
  void isExpired_만료시각이_현재와_같으면_false() {
    Instant now = Instant.parse("2026-04-19T00:00:00Z");
    RefreshToken refreshToken =
        RefreshToken.builder().user(dummyUser()).token("t1").expiryDate(now).build();

    assertFalse(refreshToken.isExpired(now));
  }

  @Test
  void isExpired_만료시각이_현재보다_이후면_false() {
    Instant now = Instant.parse("2026-04-19T00:00:00Z");
    RefreshToken refreshToken =
        RefreshToken.builder().user(dummyUser()).token("t1").expiryDate(now.plusSeconds(1)).build();

    assertFalse(refreshToken.isExpired(now));
  }

  @Test
  void updateToken_토큰값과_만료시각이_갱신된다() {
    RefreshToken refreshToken =
        RefreshToken.builder()
            .user(dummyUser())
            .token("OLD_TOKEN")
            .expiryDate(Instant.parse("2026-04-19T00:00:00Z"))
            .build();

    Instant newExpiry = Instant.parse("2026-04-20T00:00:00Z");
    refreshToken.updateToken("NEW_TOKEN", newExpiry);

    assertEquals("NEW_TOKEN", refreshToken.getToken());
    assertEquals(newExpiry, refreshToken.getExpiryDate());
  }
}
