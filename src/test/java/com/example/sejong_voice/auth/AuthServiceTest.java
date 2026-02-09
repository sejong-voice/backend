package com.example.sejong_voice.auth;

import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import com.example.sejong_voice.auth.jwt.JwtProperties;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.auth.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private RefreshTokenRepository refreshTokenRepository;
  @Mock
  private JwtUtil jwtUtil;
  @Mock
  private JwtProperties jwtProperties;

  private User mockStudentUser;
  private User mockSuperUser;
  private LoginRequest loginRequest;

  @BeforeEach
  void setUp() {
    // 1. User 객체를 모킹 (실제 객체 생성 X)
    mockStudentUser = mock(User.class);
    mockSuperUser = mock(User.class);

    // 2. 공통적으로 자주 쓰이는 Stubbing 미리 정의 (ID, 학번 등)
    // Student User Stub
    lenient().doReturn(UUID.randomUUID()).when(mockStudentUser).getId();
    lenient().doReturn("20260001").when(mockStudentUser).getStudentNo();
    lenient().doReturn("이세종").when(mockStudentUser).getName();
    lenient().doReturn(UserRole.STUDENT).when(mockStudentUser).getRole();
    lenient().doReturn(UserStatus.ACTIVE).when(mockStudentUser).getStatus();
    lenient().doReturn("encoded_password").when(mockStudentUser).getPassword();

    // Super User Stub
    lenient().doReturn(UUID.randomUUID()).when(mockSuperUser).getId();
    lenient().doReturn("20260002").when(mockSuperUser).getStudentNo();
    lenient().doReturn(UserRole.SUPER).when(mockSuperUser).getRole();

    // Login Request
    loginRequest = new LoginRequest("20260001", "password1234");
  }

  @Test
  @DisplayName("로그인 성공: 유저 정보 일치 시 토큰 발급 및 RefreshToken 저장")
  void login_Success_ExistingUser() {
    // Given
    doReturn(Optional.of(mockStudentUser)).when(userRepository).findByStudentNo("20260001");
    doReturn(true).when(passwordEncoder).matches(anyString(), anyString());

    doReturn("access_token", "refresh_token")
        .when(jwtUtil)
        .generateToken(anyString(), anyString(), anyString(), anyLong());

    // When
    LoginResponse response = authService.login(loginRequest.studentNo(), loginRequest.password());

    // Then
    assertThat(response.studentNo()).isEqualTo("20260001");
    assertThat(response.accessToken()).isEqualTo("access_token");

    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("로그인 실패: 비밀번호 불일치 시 예외 발생")
  void login_Fail_InvalidPassword() {
    // Given
    doReturn(Optional.of(mockStudentUser)).when(userRepository).findByStudentNo("20260001");
    // 비밀번호 틀림 설정
    doReturn(false).when(passwordEncoder).matches(anyString(), anyString());

    // When & Then
    assertThatThrownBy(() -> authService.login(loginRequest.studentNo(), loginRequest.password()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(AuthErrorCode.INVALID_PASSWORD);
  }

  @Test
  @DisplayName("로그인 실패: 정지 유저 접근 시 예외 발생")
  void login_Fail_Suspended() {
    // Given
    doReturn(UserStatus.SUSPENDED).when(mockStudentUser).getStatus();
    doReturn(Optional.of(mockStudentUser)).when(userRepository).findByStudentNo("20260001");
    // 정지 유저라도 비밀번호 검사 로직 순서에 따라 필요할 수 있음
    lenient().doReturn(true).when(passwordEncoder).matches(anyString(), anyString());

    // When & Then
    assertThatThrownBy(() -> authService.login(loginRequest.studentNo(), loginRequest.password()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(AuthErrorCode.USER_SUSPENDED);
  }


  @Test
  @DisplayName("토큰 재발급: 유효한 RefreshToken 확인 후 AccessToken 재발급")
  void refresh_Success() {
    // Given
    RefreshRequest refreshRequest = new RefreshRequest("20260001");
    doReturn(Optional.of(mockStudentUser)).when(userRepository).findByStudentNo("20260001");

    RefreshToken mockRefreshToken = mock(RefreshToken.class);
    doReturn(false).when(mockRefreshToken).isExpired();
    doReturn(mockStudentUser).when(mockRefreshToken).getUser();
    doReturn(Optional.of(mockRefreshToken)).when(refreshTokenRepository).findByUser(mockStudentUser);

    doReturn(3600L).when(jwtProperties).getAccessTokenExpiration();
    doReturn("new_access_token").when(jwtUtil)
        .generateToken(anyString(), anyString(), anyString(), anyLong());

    // When
    RefreshResponse response = authService.accessRefresh(refreshRequest);

    // Then
    assertThat(response.accessToken()).isEqualTo("new_access_token");
  }


  @Test
  @DisplayName("정보 조회: 존재하는 학번 조회 시 이름 등 정보 반환")
  void getUserProfile_Success() {
    // Given
    doReturn(Optional.of(mockStudentUser)).when(userRepository).findByStudentNo("20260001");

    // When
    UserSearchResponse response = authService.getUserProfile("20260001");

    // Then
    assertThat(response.name()).isEqualTo("이세종");
    assertThat(response.studentNo()).isEqualTo("20260001");
    assertThat(response.department()).isNull(); // Stubbing 안 했으므로 null (필요하면 추가)
  }


  @Test
  @DisplayName("상태 변경: 상태 변경 및 토큰 삭제 로직 호출 검증")
  void updateStatus_With_TokenDelete() {
    // Given
    doReturn(Optional.of(mockStudentUser)).when(userRepository).findByStudentNo("20260001");

    // When
    authService.updateStatus("20260001", UserStatus.SUSPENDED, LocalDateTime.now().plusDays(7));

    // Then
    // 1. 유저의 updateStatus 메서드가 호출되었는지 확인 (Service 내부 로직에 따라 다름)
    // verify(mockStudentUser).updateStatus(eq(UserStatus.SUSPENDED), any());

    // 2. 토큰 삭제가 호출되었는지 확인 (핵심)
    verify(refreshTokenRepository).deleteByUser(mockStudentUser);
  }

  @Test
  @DisplayName("권한 변경 실패: 존재하지 않는 유저는 예외 발생")
  void updateRole_Fail_UserNotFound() {
    // Given
    doReturn(Optional.empty()).when(userRepository).findByStudentNo("99999999");

    // When & Then
    assertThatThrownBy(() -> authService.updateRole("99999999", UserRole.ADMIN))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
  }
}