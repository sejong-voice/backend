package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/refresh") //현재 만료된 accessToken을 header에 담아 요청을 보내면 401 InvalidToken커스텀 예외처리가 됨.
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest refreshRequest){
        return ResponseEntity.ok(authService.accessRefresh(refreshRequest));
    }

    @GetMapping("/profile")
    //@AuthenticationPrincipal하고 @UserPrincipal로 jwt에서 값 추출한 후 principal을 만들어서 사용하는 방식. responsebody + dto 조합이 아니라
    public ResponseEntity<UserSearchResponse> getProfile(@RequestBody UserSearchRequest userSearchRequest){
        return ResponseEntity.ok(authService.getUserProfile(userSearchRequest.studentNo()));
    }
    //AuthenticationPrincipal이게 security를 통해서 인증 확인 후 인증 객체를 반환. principal 데이터 타입이랑 맞춰줘야함.

    @PostMapping("/logout")
    //@AuthenticationPrincipal하고 @UserPrincipal로 jwt에서 값 추출한 후 principal을 만들어서 사용하는 방식. responsebody + dto 조합이 아니라
    public ResponseEntity<Void> logout(@RequestBody UserSearchRequest userSearchRequest){
        authService.logout(userSearchRequest.studentNo());
        return ResponseEntity.noContent().build();
    }

}
