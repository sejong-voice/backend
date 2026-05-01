package com.example.sejong_voice.auth;

import com.example.sejong_voice.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  // 토큰 값으로 엔티티 찾기. 토큰 재발급 시 검증용
  Optional<RefreshToken> findByToken(String token);

  // 유저별 토큰 단일 관리
  Optional<RefreshToken> findByUser(User user);

  // 특정 유저의 토큰 삭제 (로그아웃)
  void deleteByToken(String token);

  void deleteByUser(User user);
}
