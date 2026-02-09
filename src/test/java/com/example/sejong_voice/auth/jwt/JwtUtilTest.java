package com.example.sejong_voice.auth.jwt;

import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SpringBootTest
public class JwtUtilTest {

  @Autowired
  private JwtUtil jwtUtil;

  @Test
  @DisplayName("토큰 생성 및 검증 (Mock 객체 사용)")
  void createAndValidateToken() {
    // given
    User user = mock(User.class);
    UUID id = UUID.randomUUID();

    doReturn(id).when(user).getId();
    doReturn("29191029").when(user).getStudentNo();
    doReturn(UserRole.STUDENT).when(user).getRole();
    doReturn("세조잉").when(user).getName();

    String token = jwtUtil.generateToken(
        user.getId().toString(),
        user.getStudentNo(),
        user.getRole().name(),
        1000L * 60 * 60
    );

    // when
    boolean isValid = jwtUtil.validateToken(token);
    Claims claims = jwtUtil.parseClaims(token);

    // then
    assertThat(isValid).isTrue();

    // 토큰에서 꺼낸 정보 검증
    assertThat(claims.getSubject()).isEqualTo(id.toString());
    assertThat(claims.get("studentNo")).isEqualTo("29191029"); // 위에서 정의한 학번
    assertThat(claims.get("role")).isEqualTo(UserRole.STUDENT.name()); // 위에서 정의한 Role
  }
}