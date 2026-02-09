package com.example.sejong_voice.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {

    String uri = request.getRequestURI();
    String token = jwtUtil.resolveToken(request);

    log.info("[JWT] {} {} tokenPresent={}", request.getMethod(), uri, token != null);

    // 토큰이 없으면 그냥 통과 (SecurityConfig가 /profile에서 막아줌)
    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // 1. 유효성 검사
      if (jwtUtil.validateToken(token)) {

        // 2. 토큰 파싱
        var authentication = jwtUtil.getAuthentication(token);

        // 3. 컨텍스트 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("[JWT] Auth Success. UserID={}, Authorities={}",
            authentication.getName(), authentication.getAuthorities());
      }
    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      log.warn("[JWT] auth process failed -> continue without authentication", e);
    }

    filterChain.doFilter(request, response);
  }
}
