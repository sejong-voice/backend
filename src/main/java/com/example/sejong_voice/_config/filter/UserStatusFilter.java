package com.example.sejong_voice._config.filter;

import com.example.sejong_voice._common.ErrorResponse;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
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
import java.time.Clock;

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
    private final Clock clock;

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
        //        다만 Authentication이 UserPrincipal일 때만 DB 상태 검사
        //authentication == null
            //아직 로그인 인증 정보가 없음(비로그인/토큰 없음/토큰 무효 등)
            //검사할 사용자 자체가 없으니 그냥 통과
        //authentication.getPrincipal() instanceof UserPrincipal가 아님
            //인증 정보는 있지만, 우리 앱이 기대하는 사용자 타입(UserPrincipal)이 아님
            //이 필터는 UserPrincipal 기준으로만 DB 상태를 검사하므로 그냥 통과

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3) principal의 userId로 DB에서 최신 유저 상태 조회
        User user = userRepository.findById(principal.getUserId()).orElseThrow(
                () -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));


        // [추가] 3-1) 탈퇴 예정(QUIT + deletedAt 존재) 차단
        if (user.getStatus() == UserStatus.QUIT && user.getDeletedAt() != null) {
            SecurityContextHolder.clearContext();
            response.setStatus(AuthErrorCode.QUIT_PENDING.getStatus()); // 403
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(AuthErrorCode.QUIT_PENDING, clock));
            return;
        }

        // 4) 유저가 없거나 ACTIVE가 아니면 접근 차단
        //    - SecurityContext 비우고
        //    - 403 + 에러 JSON 내려주고
        //    - 필터 체인 종료(return)
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            SecurityContextHolder.clearContext();
            response.setStatus(AuthErrorCode.USER_SUSPENDED.getStatus()); // 403
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(AuthErrorCode.USER_SUSPENDED, clock));
            return;
        }

        // 5) 필수 약관 미동의면 접근 차단
        //    - DB 기준 동의 여부 재검증 (service/policy 둘 다 true 필요)
        //    - SecurityContext 비우고
        //    - 403 + TERMS_NOT_AGREED 에러 JSON 내려주고
        //    - 필터 체인 종료(return)
        if (user == null || !user.hasRequiredConsents()) {
            SecurityContextHolder.clearContext();
            response.setStatus(AuthErrorCode.NOT_AGREED.getStatus()); // 403
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(AuthErrorCode.NOT_AGREED, clock));
            return;
        }

        // 5) ACTIVE 유저면 정상 통과
        filterChain.doFilter(request, response);
    }
}
