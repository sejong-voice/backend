package com.example.sejong_voice.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain filterChain
  ) throws ServletException, IOException {

    String token = jwtUtil.resolveToken(request);

    // 토큰이 없으면 빠르고 조용하게 다음 필터로 넘김
    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      Authentication authentication = jwtUtil.getAuthentication(token);

      // 정상 토큰일 경우
      SecurityContextHolder.getContext().setAuthentication(authentication);

      log.debug("[JWT] Auth Success. UserID={}", authentication.getName());

    } catch (RuntimeException e) {
      SecurityContextHolder.clearContext();
      log.warn("[JWT] Invalid Token: {}", e.getMessage());
    }
    filterChain.doFilter(request, response);
  }
}