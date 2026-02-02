package com.example.sejong_voice.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // 토큰 값으로 엔티티 찾기. 토큰 재발급 시 검증용
    Optional<RefreshToken> findByToken(String token);

    // 특정 유저의 토큰 삭제 (로그아웃)
    void  deleteByToken(String token);
}
