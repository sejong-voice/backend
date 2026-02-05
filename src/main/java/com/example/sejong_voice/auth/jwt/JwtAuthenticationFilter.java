package com.example.sejong_voice.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        //1. 헤더에서 토큰 추출
        String token = jwtUtil.resolveToken(request);

        //2. 토큰이 유효한지 검사
        if(token!=null && jwtUtil.validateToken(token)){
            // 3. 유효하다면 토큰에서 인증 정보(유저 정보)를 거내서 시큐리티 컨텍스트에 저장
            var authentication = jwtUtil.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


        //4. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
