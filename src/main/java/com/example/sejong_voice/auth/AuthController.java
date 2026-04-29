package com.example.sejong_voice.auth;


import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 API", description = "로그인/토큰 재발급/프로필 조회/로그아웃 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = "학번/비밀번호로 로그인하고 토큰 정보를 발급합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(
                authService.login(
                        loginRequest.studentNo(),
                        loginRequest.password()
                )
        );
    }

    @Operation(
            summary = "약관 동의 및 로그인",
            description = "약관 동의 결과를 받고 로그인을 진행합니다. 동의 시각은 백엔드 현재 UTC 시각으로 기록합니다."
    )
    @PostMapping("/consent-login")
    public ResponseEntity<LoginResponse> consentLogin(
            @RequestBody @Valid ConsentLoginRequest request
    ) {
        return ResponseEntity.ok(authService.consentAndLogin(request));
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token으로 Access Token을 재발급합니다."
    )
    @PostMapping("/refresh") //현재 만료된 accessToken을 header에 담아 요청을 보내면 401 InvalidToken커스텀 예외처리가 됨.
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest refreshRequest) {
        return ResponseEntity.ok(authService.accessRefresh(refreshRequest));
    }

    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @GetMapping("/profile")//조회가 필요한 user의 accessToken을 넣어서 요청해야함
    public ResponseEntity<UserSearchResponse> getProfile(
            @AuthUser UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER);
        }

        return ResponseEntity.ok(authService.getUserProfile(userPrincipal.getStudentNo()));
    }


    @Operation(
            summary = "로그아웃",
            description = "현재 로그인한 사용자를 로그아웃 처리합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthUser UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER);
        }
        authService.logout(userPrincipal.getStudentNo());
        return ResponseEntity.noContent().build();
    }

}
