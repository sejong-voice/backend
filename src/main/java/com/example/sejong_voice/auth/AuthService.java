package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import com.example.sejong_voice.auth.jwt.JwtProperties;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SchoolAuthClient schoolAuthClient;
  private final JwtUtil jwtUtil;
  private final JwtProperties jwtProperties;

  @Transactional
  public LoginResponse login(String studentNo, String password) {
    //1. DB 조회
    User user = userRepository.findByStudentNo(studentNo).orElseGet(() -> {

      // 2. DB에 유저가 없다면 파이썬 서버로 SSO 인증 요청
      SsoResponse ssoResponse = schoolAuthClient.getStudentProfile(
          new SsoRequest(studentNo, password)
      );

      // 3. sso 성공 시 받은 정보로 신규 가입
      return userRepository.save(
          User.builder()
              .studentNo(ssoResponse.user().studentNo())
              .name(ssoResponse.user().name())
              .department(ssoResponse.user().department())
              .password(passwordEncoder.encode(password))
              .build()
      );
    });

    // 4. DB에 있던 유저라면 비밀번호 검증
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
    }

    // 5. 계정 상태 체크 (정지 여부)
    if (user.getStatus() == UserStatus.SUSPENDED) {
      throw new BusinessException(AuthErrorCode.USER_SUSPENDED);
    }

    String accessToken = jwtUtil.generateToken(
        user.getId().toString(),
        user.getStudentNo(),
        user.getRole().name(),
        jwtProperties.getAccessTokenExpiration()
    );

    String refreshToken = jwtUtil.generateToken(
        user.getId().toString(),
        user.getStudentNo(),
        user.getRole().name(),
        jwtProperties.getRefreshTokenExpiration()
    );

    refreshTokenRepository.deleteByUser(user);

    // 새 토큰 정보 저장
    RefreshToken newToken = RefreshToken.builder()
        .user(user)
        .token(refreshToken)
        .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
        .build();

    refreshTokenRepository.save(newToken);

    return LoginResponse.of(
        user.getStudentNo(),
        accessToken,
        jwtProperties.getAccessTokenExpiration() / 1000
    );
  }

  // 2. 로그아웃
  @Transactional
  public void logout(String studentNo) {
    User targetUser = userRepository.findByStudentNo(studentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    refreshTokenRepository.deleteByUser(targetUser); //deleteByUser로 해도 되긴함
  }

  // 3. 토큰 재발급 (RefreshToken 유효, AccessToken 재발급)
  @Transactional
  public RefreshResponse accessRefresh(RefreshRequest refreshRequest) {
    //studentId로 유저 조회, 해당 user_id로 refreshToken 조회후 요청
    User user = userRepository.findByStudentNo(refreshRequest.studentNo())
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    //DB 존재 여부 확인
    RefreshToken storedToken = refreshTokenRepository.findByUser(user)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));//커스텀 에러

    // 토큰 유효성 검사
    if (storedToken.isExpired()) {
      refreshTokenRepository.deleteByUser(user);
      throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);//커스텀 에러
    }

    // accessToken 재발급
    User newUser = storedToken.getUser();
    String newAccessToken = jwtUtil.generateToken(
        user.getId().toString(),
        user.getStudentNo(),
        user.getRole().name(),
        jwtProperties.getAccessTokenExpiration()//3600L
    );

    return new RefreshResponse(newUser.getStudentNo(), newAccessToken,
        jwtProperties.getAccessTokenExpiration()//3600L
    );
  }

  // 4.유저 정보 조회_--------
  public UserSearchResponse getUserProfile(String studentNo) {
    User user = userRepository.findByStudentNo(studentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    return UserSearchResponse.from(user);
  }


  // 5. 유저 정보 수정 (Super 계정 전용 - 학과 수정)
  @Transactional
  public void updateDepartment(String targetStudentNo, DepartmentRequest newDepartment) {
//        if(superRole.getRole()!=UserRole.SUPER){//super계정인 경우에
//            throw new NotSuperAccountException();
//        }
    User targetUser = userRepository.findByStudentNo(targetStudentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    targetUser.updateDepartment(newDepartment);
  }

  // 6. 상태 변경 (Super 계정 전용 - 제재 및 토큰 파기)
  @Transactional
  public void updateStatus(String targetStudentNo, UserStatus newStatus,
      LocalDateTime newDuration) {
//        if(superRole.getRole()!=UserRole.SUPER){//super계정인 경우에
//            throw new NotSuperAccountException();
//        }
    User targetUser = userRepository.findByStudentNo(targetStudentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    targetUser.updateStatus(newStatus, newDuration);

    // 제재suspended이면 refreshToken 즉시파기
    if (newStatus.equals(UserStatus.SUSPENDED)) {
      refreshTokenRepository.deleteByUser(targetUser);
    }
  }

  // 7. 권한 수정 (Super 계정 전용 - Role 변경)
  @Transactional
  public void updateRole(String targetStudentNo, UserRole newRole) {
//        // 1. 권한이 SUPER가 아니면 즉시 예외 발생
//        if (superRole.getRole() != UserRole.SUPER) {
//            throw new NotSuperAccountException(); //
//        }

    // 2. 권한이 확인된 경우에만 이후 로직 실행
    User targetUser = userRepository.findByStudentNo(targetStudentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    targetUser.updateRole(newRole);
  }
}