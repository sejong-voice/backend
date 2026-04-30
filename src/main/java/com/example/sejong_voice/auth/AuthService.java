package com.example.sejong_voice.auth;


import com.example.sejong_voice._common.time.TimeUtils;
import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.affiliation.AffiliationRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AffiliationRepository affiliationRepository;
  private final SchoolAuthClient schoolAuthClient;
  private final JwtUtil jwtUtil;
  private final JwtProperties jwtProperties;
  private final Clock clock;

  private User authenticateOrCreateUser(String studentNo, String password) {
    //1. DB 조회
    return userRepository.findByStudentNo(studentNo)
        .orElseGet(() -> {
          // 2. DB에 유저가 없다면 파이썬 서버로 SSO 인증 요청
          SsoResponse ssoResponse;

          try {
            ssoResponse = schoolAuthClient.getStudentProfile(
                new SsoRequest(studentNo, password)
            );
          } catch (BusinessException e) {
            // 이미 비즈니스 예외로 올라온 건 그대로 전달
            throw e;
          } catch (Exception e) {
            // 네트워크/파싱/런타임 등은 SSO 실패로 통일
            throw new BusinessException(AuthErrorCode.SSO_AUTH_FAILED);
          }

          // null 응답 방어
          if (ssoResponse == null || ssoResponse.user() == null) {
            throw new BusinessException(AuthErrorCode.SSO_AUTH_FAILED);
          }

          Affiliation affiliation = affiliationRepository.findByName(
                  ssoResponse.user().department())
              .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_AFFILIATION));

          // 3. sso 성공 시 받은 정보로 신규 가입
          Instant now = Instant.now(clock);

          return userRepository.save(
              User.builder()
                  .studentNo(ssoResponse.user().studentNo())
                  .name(ssoResponse.user().name())
                  .affiliation(affiliation)
                  .password(passwordEncoder.encode(password))
                  .duration(User.activeDuration())
                  .createdAt(now)
                  .build()
          );
        });
  }

  private void validatePasswordWithSsoFallback(User user, String studentNo, String password) {
    SsoResponse ssoResponse;
    // 4. DB에 있던 유저라면 비밀번호 검증 -> 비번 틀리면 다시 sso요청, 예외
    if (!passwordEncoder.matches(password, user.getPassword())) {
      // SSO 재검증: 여기서 실패하면 예외 던지도록 매핑
      try {
        ssoResponse = schoolAuthClient.getStudentProfile(new SsoRequest(studentNo, password));
      } catch (Exception e) {
        throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
      }

      if (ssoResponse == null || ssoResponse.user() == null) {
        throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
      }

      // SSO 성공이면 비번 동기화 후 통과
      user.updatePassword(passwordEncoder.encode(password));
      // ssoResponse.user() 이런식으로는 할 필요가 없는게 sso를 예외없이 통과된거라 검증된 비번이기 때문
      userRepository.save(user);
    }
  }

  private void validateUserStatus(User user) {
    // 5. 계정 상태 체크 (정지 여부, duration 만료하면 acitvate+duration 초기화)
    if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.WITHDRAWN) {
      Instant now = Instant.now(clock);
      if (now.isBefore(user.getDuration())) {
        throw new BusinessException(AuthErrorCode.USER_SUSPENDED);
      } else {
        user.updateStatus(UserStatus.ACTIVE, User.activeDuration());
      }
    }
  }

  private LoginResponse issueLoginTokens(User user) {

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

  private void validateConsentsOrClearToken(User user) {
    //login() refresh()에서 사
    //미동의 사용자 공통 차단
    //미동의인데 남아있을 수 있는 refresh token 즉시 정리
    if (user.hasRequiredConsents()) {
      return;
    }

    // 미동의 상태면 기존 세션(리프레시 토큰) 강제 정리
    refreshTokenRepository.deleteByUser(user);

    // 로그인/재발급 모두 중단
    throw new BusinessException(AuthErrorCode.NOT_AGREED);
  }

  private void validateQuitPending(User user) {
    if (user.getStatus() == UserStatus.QUIT && user.getDeletedAt() == null) {
      // 데이터 정합성 깨진 케이스 감지용
      // Quit인데 deleated_at이 null
      // -> 로직에서는 해당 케이스가 발생하진 않지만 혹시나 정합성이 깨질 수 있으니, 확인 하기 위함
      log.warn("Inconsistent QUIT user detected. userId={}", user.getId());
    }
    // 현재 탈퇴 처리가 되었다면 login(), constent-login(), refresh()에서 필터링하기 위함
    if (user.getStatus() == UserStatus.QUIT && user.getDeletedAt() != null) {
      refreshTokenRepository.deleteByUser(user); // 방어적으로 세션 정리
      throw new BusinessException(AuthErrorCode.QUIT_PENDING);
    }
  }


  @Transactional
  public LoginResponse login(String studentNo, String password) {
    // 1..3 db조회, 없으면 sso 신규 가입
    User user = authenticateOrCreateUser(studentNo, password);
    // 4.비밀번호 검증 -> 비번 틀리면 다시 sso요청
    validatePasswordWithSsoFallback(user, studentNo, password);
    // 5. 계정 정지 여부 확인
    validateUserStatus(user);
    // 7. 탈퇴 계정 확인
    validateQuitPending(user);
    // 6. 서비스, 이용약관 동의 여부 체크 -> refreshToken만 삭제하고 sso에 따른 user정보 삭제는 안함
    validateConsentsOrClearToken(user);


    return issueLoginTokens(user);
  }

  @Transactional
  public LoginResponse consentAndLogin(ConsentLoginRequest request) {
    //약관 동의 및 로그인
    // 프론트에서 둘 다 true로 보냈는지 검증
    if (!Boolean.TRUE.equals(request.serviceAgreed())
        || !Boolean.TRUE.equals(request.policyAgreed())) {
      throw new BusinessException(AuthErrorCode.NOT_AGREED);
    }

    // 1..3 db조회, 없으면 sso 신규 가입
    User user = authenticateOrCreateUser(request.studentNo(), request.password());
    // 4.비밀번호 검증 -> 비번 틀리면 다시 sso요청
    validatePasswordWithSsoFallback(user, request.studentNo(), request.password());
    // 5. 계정 정지 여부 확인
    validateUserStatus(user);
    // 7. 탈퇴 계정 확인
    validateQuitPending(user);

    // 6. 서비스, 이용약관 동의 여부 체크는 안함 -> 약관 동의 및 로그인이라서, 약관 동의가 안되어있음.
    // 동의 시각은 백엔드 현재 UTC 시각으로 저장
    Instant now = Instant.now(clock);
    user.agreeRequiredConsents(now, now);

    return issueLoginTokens(user);
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

    // 7. 탈퇴 계정 확인 -> 탈퇴 계정은 실제 hard deleted 전까진 db에 기록이 남아있어 서비스 이용 못함. refresh토큰 재발급해줄 필요가 없음
    validateQuitPending(user);
    // 서비스, 이용약관 동의 유무 검증
    validateConsentsOrClearToken(user);


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
    } else if (newStatus.equals(UserStatus.QUIT)) { // 탈퇴 처리 (duration은 보내줘도 안씀, 그래도 넣긴해야함)
      if(targetUser.isQuitPending()){ //이미 탈퇴처리가 됐다면, 예외처리
        throw new BusinessException(AuthErrorCode.QUIT_PENDING);
      }
      targetUser.markQuit(Instant.now(clock));
      refreshTokenRepository.deleteByUser(targetUser); // 토큰 파기까지 (실제 삭제는 안되고 사용 못하게 막는 용도)

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

  // 8. 최종 탈퇴
  @Transactional
  public void requestQuit(String targetStudentNo) {
    User user = userRepository.findByStudentNo(targetStudentNo)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    if(user.isQuitPending()){ //이미 탈퇴처리가 됐다면, 예외처리
      throw new BusinessException(AuthErrorCode.QUIT_PENDING);
    }

    user.markQuit(Instant.now(clock));
    refreshTokenRepository.deleteByUser(user); // 토큰 파기까지 (실제 삭제는 안되고 사용 못하게 막는 용도)
  }
}
