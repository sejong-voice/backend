package com.example.sejong_voice.jwt;


import static org.junit.jupiter.api.Assertions.*;

import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.auth.jwt.JwtProperties;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

class JwtUtilTest {
    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(
                "sejong-voice",
                "12345678901234567890123456789012", // 32bytes+
                3_600_000L,
                1_209_600_000L
        );

        jwtUtil = new JwtUtil(jwtProperties);
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");
    }

    @Test
    void 토큰생성_및_검증_정상() {
        //given
        String token = jwtUtil.generateToken(
                UUID.randomUUID().toString(),
                "20201234",
                "STUDENT",
                60_000L
        );

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void 잘못된토큰은_validate_false() {
        assertFalse(jwtUtil.validateToken("invalid_token"));
    }

    @Test
    void 토큰클레임_파싱_정상() {
        //given
        String userId =UUID.randomUUID().toString();
        String token = jwtUtil.generateToken(
                userId,
                "20201234",
                "STUDENT",
                60_000L
        );

        //when
        Claims claims = jwtUtil.parseClaims(token);

        //then
        assertNotNull(claims);
        assertEquals(userId, claims.getSubject());
        assertEquals("20201234", claims.get("studentNo", String.class));
        assertEquals("STUDENT", claims.get("role", String.class));
    }



    @Test
    void Authorization헤더에서_Bearer토큰_추출() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc.def.ghi");

        assertEquals("abc.def.ghi", jwtUtil.resolveToken(request));
    }


    @Test
    void Bearer형식아니면_토큰추출_null() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");

        assertNull(jwtUtil.resolveToken(request));
    }


    @Test
    void getAuthentication_정상토큰이면_Authentication생성() {
        //given
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId.toString(), "20201234", "STUDENT", 60_000L);


        //when
        Authentication authentication = jwtUtil.getAuthentication(token);

        //then
        assertNotNull(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertEquals(userId, principal.getUserId());
        assertEquals("20201234", principal.getStudentNo());
        assertEquals("STUDENT", principal.getRole().toString());

        assertTrue(authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_STUDENT")));

    }
    // getAuthentication에 token을 넘겨줌. 반환값으로 authentication을 받음, 안되는 케이스에 대해서는 해당 메서드들 (정상, 비정상)으로 테스트함
    // authentication에서 principal 객체를 꺼냄. principal 객체의 하위 값들이랑 임의로 만든 토큰 내부의 값들이랑 동일한지 확인
    // -> 이를 통해 header에서 token을 파싱, 파싱한 토큰에서 값들을 정상 파싱하여 principal 인증 객체를 만드는 것을 최종 확인
}

