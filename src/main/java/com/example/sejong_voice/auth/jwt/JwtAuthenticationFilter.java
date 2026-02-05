//package com.example.sejong_voice.auth.jwt;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//    private final JwtUtil jwtUtil;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        //1. 헤더에서 토큰 추출
//        String token = jwtUtil.resolveToken(request);
//
//        //2. 토큰이 유효한지 검사
//        if(token!=null && jwtUtil.validateToken(token)){
//            // 3. 유효하다면 토큰에서 인증 정보(유저 정보)를 거내서 시큐리티 컨텍스트에 저장
//            var authentication = jwtUtil.getAuthentication(token);
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//        }
//
//
//        //4. 다음 필터로 진행
//        filterChain.doFilter(request, response);
//    }
//}


package com.example.sejong_voice.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String token = jwtUtil.resolveToken(request);

        log.info("[JWT] {} {} tokenPresent={}", request.getMethod(), uri, token != null);

        // 토큰이 없으면 그냥 통과 (SecurityConfig가 /profile에서 막아줌)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 검증/파싱/Authentication 생성은 예외가 나기 쉬운 구간이라 모두 방어
        try {
            boolean valid = jwtUtil.validateToken(token);
            log.info("[JWT] valid={}", valid);

            if (valid) {
                Claims c = jwtUtil.parseClaims(token);
                log.info("[JWT] sub={}, role={}", c.getSubject(), c.get("role"));

                var authentication = jwtUtil.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("[JWT] authenticationSet=true");
            }

        } catch (Exception e) {
            // 여기서 throw하면 500으로 커질 확률이 큼
            SecurityContextHolder.clearContext();
            log.warn("[JWT] auth process failed -> continue without authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}
