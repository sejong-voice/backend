package com.example.sejong_voice.service;


// 1. JUnit 5 & Mockito 관련
import com.example.sejong_voice.auth.*;
import com.example.sejong_voice.auth.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 2. 검증(Assertion) 관련 - AssertJ
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 3. 스프링 및 보안 관련
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

// 4. 프로젝트 도메인 및 예외 관련 (본인의 패키지 경로에 맞게 확인 필요)
import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.global.error.exception.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private SchoolAuthClient schoolAuthClient;
    @Mock private JwtUtil jwtUtil;
    @Mock private JwtProperties jwtProperties;

    private User studentUser;
    private User superUser;
    private LoginRequest loginRequest;
    private UserRoleRequest userSearchRoleRequest;

    @BeforeEach
    void setUp() {
        //일반 학생 유저 셋팅
        studentUser = User.builder()
                .studentNo("20260001")
                .name("이세종")
                .department("컴퓨터공학과")
                .password("encoded_password")
                .build();
        // Super 관리자 유저 셋팅
        superUser = User.builder()
                .studentNo("20260002")
                .name("개발자")
                .department("컴퓨터공학과")
                .password("1234")
                .build();
        ReflectionTestUtils.setField(superUser, "role",UserRole.SUPER);

        userSearchRoleRequest = new UserRoleRequest(superUser.getRole(),superUser.getStudentNo());
        // login dto 셋팅
        loginRequest = new LoginRequest("20260001","password1234");//비번 틀리게 로그인
    }

    // 1. 로그인 시나리오
    @Test
    @DisplayName("로그인 성공: 기존 유저 정보 일치 시 토큰 발급")
    void login_Success_ExistingUser() {
        given(userRepository.findByStudentNo(anyString())).willReturn(Optional.of(studentUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(jwtUtil.generateToken(any(), anyLong())).willReturn("access_token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.studentNo()).isEqualTo("20260001");
        assertThat(response.accessToken()).isEqualTo("access_token");
        verify(refreshTokenRepository).save(any()); // 리프레시 토큰 저장 확인
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호 불일치 시 InvalidPasswordException 발생")
    void login_Fail_InvalidPassword() {
        given(userRepository.findByStudentNo(anyString())).willReturn(Optional.of(studentUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidPasswordException.class); // 커스텀 에러 검증
    }

    @Test
    @DisplayName("로그인 실패: 정지 유저 접근 시 UserSuspendedException 발생")
    void login_Fail_Suspended() {
        ReflectionTestUtils.setField(studentUser, "status", UserStatus.SUSPENDED);
        given(userRepository.findByStudentNo(anyString())).willReturn(Optional.of(studentUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UserSuspendedException.class);
    }


    // 2. 토큰 및 정보 조회
    // 토큰 관련 테스트가 재발급 말고도 더 있을 것 같은데
    @Test
    @DisplayName("토큰 재발급: 유효한 RefreshToken으로 새 AccessToken 반환")
    void refresh_Success() {
        // 유효한 리프레시 토큰을 주입하고 새 액세스 토큰이 반환되는지 검증
        RefreshToken rt = RefreshToken.builder()
                .user(studentUser).token("valid_rt").expiryDate(Instant.now().plus(Duration.ofDays(1))).build();

        // 2. 가짜 DB(Repository)의 행동을 정의합니다. (Stubbing)
        // 어떤 문자열이 들어오더라도 위에서 만든 가짜 토큰(rt)을 담은 Optional을 반환하도록 설정합니다.
        given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.of(rt));

        // 3. 토큰 생성 도구(JwtUtil)의 행동을 정의합니다.
        // 유저 정보와 만료 시간이 전달되면 무조건 "new_at"이라는 문자열을 반환하도록 설정합니다.
        given(jwtUtil.generateToken(any(), anyLong())).willReturn("new_at");

        // 4. 실제 테스트 대상 메서드(accessRefresh)를 호출합니다.
        // "valid_rt"라는 토큰을 전달했을 때 서비스 로직이 정상적으로 돌아가는지 확인합니다.
        RefreshResponse response = authService.accessRefresh("valid_rt");

        // 5. 결과가 예상과 일치하는지 검증(Assertion)합니다.
        // 서비스로부터 받은 응답의 액세스 토큰 값이 우리가 설정한 "new_at"과 같은지 확인합니다.
        assertThat(response.accessToken()).isEqualTo("new_at");
    }

    @Test
    @DisplayName("정보 조회: 존재하는 학번 조회 시 DTO 반환")
    void getUserProfile_Success() {
        given(userRepository.findByStudentNo(anyString())).willReturn(Optional.of(studentUser));

        UserSearchResponse response = authService.getUserProfile("20260001");

        assertThat(response.name()).isEqualTo("이세종");
    }


    // --- [3. Super 계정 관리 기능] ---
    @Test
    @DisplayName("상태 변경: 제재(SUSPENDED) 시 해당 유저의 RefreshToken 즉시 파기")
    void updateStatus_With_TokenDelete() {
        // 1. Given: DTO를 SUPER 권한으로 확실하게 셋팅
        UserRoleRequest adminRequest = new UserRoleRequest(UserRole.SUPER,superUser.getStudentNo());

        given(userRepository.findByStudentNo("20260001")).willReturn(Optional.of(studentUser));

        // 2. When: 위에서 만든 adminRequest를 전달
        authService.updateStatus(adminRequest, "20260001", UserStatus.SUSPENDED, LocalDateTime.now().plusDays(7));

        // 3. Then: 검증
        verify(refreshTokenRepository).deleteByUser(studentUser);
    }

    @Test
    @DisplayName("권한 차단: STUDENT 권한 유저가 관리 기능을 호출하면 NotSuperAccountException 발생")
    void updateRole_Fail_ByStudent() {
        // Given: 호출자(superRole)의 권한을 STUDENT로 설정
        ReflectionTestUtils.setField(userSearchRoleRequest, "role", UserRole.STUDENT);

        // When & Then: 예외가 발생하는지 검증
        assertThatThrownBy(() ->
                authService.updateRole(userSearchRoleRequest, "20260002", UserRole.ADMIN)
        ).isInstanceOf(NotSuperAccountException.class);

        // Then: 예외가 먼저 터졌으므로 DB 조회(findByStudentNo)는 단 한 번도 일어나지 않아야 함
        verify(userRepository, never()).findByStudentNo(anyString());
    }
}
