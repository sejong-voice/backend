package com.example.sejong_voice.auth;



import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
    return ResponseEntity.ok(
        authService.login(
            loginRequest.studentNo(),
            loginRequest.password()
        )
    );
  }

  @PostMapping("/refresh") //현재 만료된 accessToken을 header에 담아 요청을 보내면 401 InvalidToken커스텀 예외처리가 됨.
  public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest refreshRequest) {
    return ResponseEntity.ok(authService.accessRefresh(refreshRequest));
  }

  @GetMapping("/profile")//조회가 필요한 user의 accessToken을 넣어서 요청해야함
  public ResponseEntity<UserSearchResponse> getProfile() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      //토큰이 없거나 JWT 검증에 실패해 SecurityContext가 비어 있는 경우
      //혹은 다른 인증 방식으로 들어와 principal 타입이 UserPrincipal이 아닌 경우
      throw new BusinessException(AuthErrorCode.INVALID_USER);
    }

    return ResponseEntity.ok(authService.getUserProfile(principal.getStudentNo()));
  }



  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      throw new BusinessException(AuthErrorCode.INVALID_USER);
    }
    authService.logout(principal.getStudentNo());
    return ResponseEntity.noContent().build();
  }

}
