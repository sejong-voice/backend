package com.example.sejong_voice.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.sejong_voice._config.filter.JwtAuthenticationFilter;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtUtil jwtUtil;

  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtUtil);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 토큰없으면_체인통과() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(jwtUtil.resolveToken(any())).thenReturn(null);

    // when
    filter.doFilter(request, response, filterChain);

    // then
    verify(jwtUtil).resolveToken(any()); // 테스트 실행 중에 jwtUtil.resolveToken(...) 이 호출됐는지 확인
    verify(jwtUtil, never()).getAuthentication(anyString());
    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void 유효토큰이면_SecurityContext에_인증설정후_체인통과() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String token = "valid-token";

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "principal", token, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

    when(jwtUtil.resolveToken(any())).thenReturn(token);
    when(jwtUtil.getAuthentication(token)).thenReturn(authentication);

    // when
    filter.doFilter(request, response, filterChain);

    // then
    assertEquals(SecurityContextHolder.getContext().getAuthentication(), authentication);
    verify(jwtUtil).getAuthentication(token);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void 무효토큰이면_ContextClear하고_체인통과() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String badToken = "bad-token";

    when(jwtUtil.resolveToken(any())).thenReturn(badToken);
    when(jwtUtil.getAuthentication(badToken)).thenThrow(new RuntimeException("invalid token"));

    // when
    filter.doFilter(request, response, filterChain);

    // then
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtUtil).getAuthentication(badToken);
    verify(filterChain).doFilter(request, response);
  }
}
