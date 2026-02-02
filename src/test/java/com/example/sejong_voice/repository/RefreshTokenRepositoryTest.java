package com.example.sejong_voice.repository;


import com.example.sejong_voice.auth.RefreshToken;
import com.example.sejong_voice.auth.RefreshTokenRepository;
import com.example.sejong_voice.auth.User;
import com.example.sejong_voice.auth.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

@SpringBootTest
@Transactional
public class RefreshTokenRepositoryTest {
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    void saveFindAndDeleteRefreshToken() {
        User user = User.builder()
                .studentNo("1231231")
                .name("밥밥밥")
                .department("컴퓨터공학과")
                .build();
        userRepository.save(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("tokenTest")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        RefreshToken refreshToken2 = RefreshToken.builder()
                .user(user)
                .token("tokenTest")
                .expiryDate(Instant.now().plusSeconds(1))
                .build();

        refreshTokenRepository.save(refreshToken);

        assertThat(refreshToken.getId()).isNotNull();

        assertThat(refreshTokenRepository.findByToken("tokenTest").get().getToken())
                .isEqualTo(refreshToken.getToken());

        refreshTokenRepository.deleteByToken("tokenTest");
        assertThat(refreshTokenRepository.findByToken("tokenTest")).isEmpty();


    }

    @Test
    @DisplayName("동일 유저에게 두 개 이상의 토큰 저장 시 예외 발생")
    void uniqueUserConstraint(){
        User user1 = User.builder().studentNo("1231231").name("밥밥밥1").department("컴퓨터공학과").build();
        userRepository.save(user1);
        refreshTokenRepository.save(RefreshToken.builder().
                user(user1).token("dup-token").expiryDate(Instant.now().plusSeconds(300)).build()
        );

        //동일 유저로 또 다른 토큰 저장 시도
        assertThatThrownBy(() -> {
            refreshTokenRepository.saveAndFlush(RefreshToken.builder()
                    .user(user1).token("dup-token2").expiryDate(Instant.now().plusSeconds(300)).build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("만료 여부 확인 및 토큰 갱신 로직 테스트")
    void expireAndUpdateTokenBehavior(){
        User user = userRepository.save(User.builder().studentNo("1231231").name("밥밥밥1").department("컴퓨터공학과").build());

        //1. 이미 만료된 토큰 저장
        RefreshToken token = refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token("will-expire")
                .expiryDate(Instant.now().minusSeconds(10))
                .build());

        assertTrue(token.isExpired(),"과거 시각이면 완료로 판단되어야 함");

        //2. 토큰 갱신 로직 실행 (Entity 내부에 updateToken 메서드가 있다고 가정)
        token.updateToken("renewed-token",Instant.now().plusSeconds(10));
        refreshTokenRepository.saveAndFlush(token);

        //3. 결과 확인
        RefreshToken refreshed = refreshTokenRepository.findByToken("renewed-token").orElseThrow();
        assertFalse(refreshed.isExpired(),"갱신된 후에는 만료가 아니어야함");
        assertThat(refreshed.getToken()).isEqualTo("renewed-token");
    }

}
