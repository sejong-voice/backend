package com.example.sejong_voice.auth;


import com.example.sejong_voice._common.time.TimeUtils;
import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import com.example.sejong_voice.auth.jwt.JwtProperties;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.user.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
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
  private final Clock clock;

  @Transactional
  public LoginResponse login(String studentNo, String password) {
    //1. DB 조회
    User user = userRepository.findByStudentNo(studentNo)
            .orElseGet(() -> {

      // 2. DB에 유저가 없다면 파이썬 서버로 SSO 인증 요청
      SsoResponse ssoResponse = schoolAuthClient.getStudentProfile(
          new SsoRequest(studentNo, password)
      );

      // 3. sso 성공 시 받은 정보로 신규 가입
      Instant now = Instant.now(clock);
      return userRepository.save(
          User.builder()
              .studentNo(ssoResponse.user().studentNo())
              .name(ssoResponse.user().name())
              .department(ssoResponse.user().department())
              .password(passwordEncoder.encode(password))
              .duration(User.activeDuration())
              .createdAt(now)
              .build()
      );
    });

    // 4. DB에 있던 유저라면 비밀번호 검증 -> 비번 틀리면 다시 sso요청, 예외
    if (!passwordEncoder.matches(password, user.getPassword())) {
      // SSO 재검증: 여기서 실패하면 예외 던지도록 매핑
      try {
        schoolAuthClient.getStudentProfile(new SsoRequest(studentNo, password));
      } catch (Exception e) {
        throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
      }

      // SSO 성공이면 비번 동기화 후 통과
      user.updatePassword(passwordEncoder.encode(password));
      // ssoResponse.user() 이런식으로는 할 필요가 없는게 sso를 예외없이 통과된거라 검증된 비번이기 때문
      userRepository.save(user);
    }

    // 5. 계정 상태 체크 (정지 여부, duration 만료하면 acitvate+duration 초기화)
    if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.WITHDRAWN) {
      Instant now = Instant.now(clock);
      if( now.isBefore(user.getDuration())){
        throw new BusinessException(AuthErrorCode.USER_SUSPENDED);
      } else{
        user.updateStatus(UserStatus.ACTIVE, User.activeDuration());
      }
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


    Instant newExpiry = Instant.now(clock).plusMillis(jwtProperties.getRefreshTokenExpiration());

    //refreshToken 조회 후 있으면 update, 없으면 신규 생성 후 save
    refreshTokenRepository.findByUser(user).ifPresentOrElse(
            existingToken -> existingToken.updateToken(refreshToken, newExpiry),
            () -> refreshTokenRepository.save(
                    RefreshToken.builder()
                            .user(user)
                            .token(refreshToken)
                            .expiryDate(newExpiry)
                            .build()
            )
    );

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
    if (storedToken.isExpired(Instant.now(clock))) {
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
        jwtProperties.getAccessTokenExpiration() / 1000
    );
  }

  // 4.유저 정보 조회
  public UserSearchResponse getUserProfile(String studentNo) {
    User user = userRepository.findByStudentNo(studentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    return UserSearchResponse.from(user);
  }


  // 5. 유저 정보 수정 (Super 계정 전용 - 학과 수정)
  @Transactional
  public void updateDepartment(
          String targetStudentNo,
          DepartmentRequest newDepartment
  ) {
    User targetUser = userRepository.findByStudentNo(targetStudentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    targetUser.updateDepartment(newDepartment);
  }

  // 6. 상태 변경 (Super 계정 전용 - 제재 및 토큰 파기)
  @Transactional
  public void updateStatus(
          String targetStudentNo,
          UserStatus newStatus,
          int newDurationdays
  ) {
    User targetUser = userRepository.findByStudentNo(targetStudentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));


    long suspendSeconds = newDurationdays * 24L * 60 * 60; // int 오버플로우 방지용 long
    Instant suspendedUntilUtc = Instant.now(clock).plusSeconds(suspendSeconds); // UTC 기준


    // 제재suspended, withdrawn일때만 duration업데이트 및 refreshToken 즉시파기
    if (newStatus.equals(UserStatus.SUSPENDED)) {// 제재 기한 제공
      targetUser.updateStatus(newStatus, suspendedUntilUtc);
      refreshTokenRepository.deleteByUser(targetUser);
    } else if (newStatus.equals(UserStatus.WITHDRAWN)) { // 영구 정지
      targetUser.withdrawNow();
      refreshTokenRepository.deleteByUser(targetUser);
    }
  }

  // 6-2. 조기 복구 (Super 계정 전용)
  @Transactional
  public void activateStatus(String targetStudentNo) {
    User targetUser = userRepository.findByStudentNo(targetStudentNo)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    targetUser.activateNow();
  }

  // 7. 권한 수정 (Super 계정 전용 - Role 변경)
  @Transactional
  public void updateRole(
          String targetStudentNo,
          UserRole newRole
  ) {
    User targetUser = userRepository.findByStudentNo(targetStudentNo)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    targetUser.updateRole(newRole);
  }
}
