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

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest refreshRequest){
        return ResponseEntity.ok(authService.accessRefresh(refreshRequest.refreshToken()));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserSearchResponse> getProfile(@AuthenticationPrincipal UserSearchRequest userSearchRequest){
        return ResponseEntity.ok(authService.getUserProfile(userSearchRequest.studentNo()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserSearchRequest userSearchRequest){
        authService.logout(userSearchRequest.studentNo());
        return ResponseEntity.noContent().build();
    }

}
