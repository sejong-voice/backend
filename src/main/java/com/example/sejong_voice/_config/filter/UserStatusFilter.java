package com.example.sejong_voice._config.filter;

import com.example.sejong_voice._common.ErrorResponse;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class UserStatusFilter extends OncePerRequestFilter {


    // DB에서 현재 유저 상태(ACTIVE/SUSPENDED/WITHDRAWN) 조회용
    private final UserRepository userRepository;

    // 에러 응답을 JSON으로 직렬화하기 위한 객체
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1) JwtAuthenticationFilter가 넣어둔 인증 정보(SecurityContext) 꺼내기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2) 비로그인 요청이거나 principal 타입이 UserPrincipal이 아니면
        //    이 필터에서는 검사할 게 없으므로 다음 필터로 통과
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3) principal의 userId로 DB에서 최신 유저 상태 조회
        User user = userRepository.findById(principal.getUserId()).orElse(null);

        // 4) 유저가 없거나 ACTIVE가 아니면 접근 차단
        //    - SecurityContext 비우고
        //    - 403 + 에러 JSON 내려주고
        //    - 필터 체인 종료(return)
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            SecurityContextHolder.clearContext();
            response.setStatus(AuthErrorCode.USER_SUSPENDED.getStatus()); // 403
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(AuthErrorCode.USER_SUSPENDED));
            return;
        }

        // 5) ACTIVE 유저면 정상 통과
        filterChain.doFilter(request, response);
    }
}
