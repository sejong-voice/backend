package com.example.sejong_voice.security;

import com.example.sejong_voice.auth.User;
import com.example.sejong_voice.auth.UserRole;
import com.example.sejong_voice.global.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
public class JwtUtilTest {

    @Autowired private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .studentNo("29191029")
                .name("세조잉")
                .department("컴퓨터공학과")
                .build();
    }

    @Test
    @DisplayName("토큰 생성 및 검증")
    void createAndValidateToken(){
        //given
        String token = jwtUtil.generateToken(testUser,1000L*60);
        //when
        boolean isValid = jwtUtil.validateToken(token);
        Claims claims =jwtUtil.parseClaims(token); //이건 왜해주는거야? 반환값이 뭔데. token 정보반환

        //then
        assertThat(isValid).isTrue();
        assertThat(claims.getSubject()).isEqualTo(testUser.getStudentNo());//token에 명시된 객체(user)가 studentno인지
        assertThat(claims.get("role")).isEqualTo(UserRole.STUDENT.name());// claim("role",user.getRole().name()) user의 role을 가져온거라 맞는지 확인.
    }


}
