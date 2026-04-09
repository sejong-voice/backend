package com.example.sejong_voice.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.auth.jwt.JwtProperties;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  // AuthService가 의존하는 컴포넌트는 모두 Mock으로 주입해
  // 로그인 로직만 독립적으로 검증한다.
  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private RefreshTokenRepository refreshTokenRepository;
  @Mock
  private SchoolAuthClient schoolAuthClient;
  @Mock
  private JwtUtil jwtUtil;

  private AuthService authService;
  private JwtProperties jwtProperties;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    // 고정 시간, jwt 설정값, 임시 테스트 서비스

    // 시간 의존 로직(정지 기간 비교, 토큰 만료 계산)을
    // 매번 동일하게 검증하기 위해 고정 시계를 사용한다.
    fixedClock = Clock.fixed(Instant.parse("2026-04-07T00:00:00Z"), ZoneOffset.UTC);

    // 테스트 전용 JWT 설정값.
    // access/refresh 만료시간을 명시해 응답 expiresIn 검증에 사용한다.
    jwtProperties = new JwtProperties(
        "sejong-voice",
        "12345678901234567890123456789012",
        3_600_000L,
        1_209_600_000L
    );

    // 테스트 대상 서비스 생성
    authService = new AuthService(
        userRepository,
        passwordEncoder,
        refreshTokenRepository,
        schoolAuthClient,
        jwtUtil,
        jwtProperties,
        fixedClock
    );
  }
  private User buildUser(String studentNo, String encodedPassword) {
    // 테스트에서 반복 생성되는 유저 fixture.
    // 생성 시 기본 상태는 ACTIVE이며, 필요 시 각 테스트에서 상태를 변경한다.
    User user = User.builder()
            .studentNo(studentNo)
            .name("홍길동")
            .department("컴퓨터공학과")
            .password(encodedPassword)
            .duration(User.activeDuration())
            .createdAt(Instant.now(fixedClock))
            .build();

    // JPA 저장 전 엔티티를 흉내내기 위해 임의 UUID를 주입한다.
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }

  //login
  @Test
  void 신규_유저_로그인_시_SSO_가입후_토큰발급() {
    // given: DB에 사용자가 없는 로그인 요청
    String studentNo = "20201234";
    String rawPassword = "pw";

    // DB 미존재 -> SSO 호출 분기로 진입
    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.empty());
    // SSO 응답 성공 데이터
    when(schoolAuthClient.getStudentProfile(any(SsoRequest.class))).thenReturn(
        new SsoResponse(
            new SsoUser("1", studentNo, "student", "홍길동", "컴퓨터공학과", 2),
            new SsoAuthResult(true, true, "OK", Map.of())
        )
    );
    // 신규 저장 시 비밀번호 암호화
    when(passwordEncoder.encode(rawPassword)).thenReturn("ENC_PW");
    // save 시 JPA처럼 ID가 채워진 상태를 흉내낸다.
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      if (user.getId() == null) {
        ReflectionTestUtils.setField(
            user,
            "id",
            UUID.fromString("00000000-0000-0000-0000-000000000001")
        );
      }
      return user;
    });
    // 로그인 성공 후 발급될 access/refresh 토큰 값 stub
    when(jwtUtil.generateToken(anyString(), eq(studentNo), anyString(),
        eq(jwtProperties.getAccessTokenExpiration()))).thenReturn("ACCESS_TOKEN");
    when(jwtUtil.generateToken(anyString(), eq(studentNo), anyString(),
        eq(jwtProperties.getRefreshTokenExpiration()))).thenReturn("REFRESH_TOKEN");
    // 기존 refresh token 없음 처리 -> 신규 저장 필요
    when(refreshTokenRepository.findByUser(any(User.class))).thenReturn(Optional.empty());
    // 따로 처리해줄 필요는 없음. authservice에도 해당 로직처리는 없음.
    // 하지만 만약에 refreshToken으로 조회가 들어오면, 우리는 db에 없는 신규유저임을 가정하고 있기에 refreshToken이 없음을 반환처리

    // when
    LoginResponse response = authService.login(studentNo, rawPassword);

    // then: 응답 필드 및 refresh token 저장 여부 검증
    assertEquals(studentNo, response.studentNo());
    assertEquals("ACCESS_TOKEN", response.accessToken());
    assertEquals(jwtProperties.getAccessTokenExpiration() / 1000, response.expiresIn());
    verify(refreshTokenRepository).save(any(RefreshToken.class));

    System.out.println("response.studentNo = " + response.studentNo());
    System.out.println("response.accessToken = " + response.accessToken());
    System.out.println("response.expiresIn = " + response.expiresIn());
  }

  @Test
  void 기존유저_비밀번호불일치_SSO재검증실패면_INVALID_PASSWORD() {
    // given: DB에는 사용자 존재하지만 비밀번호 불일치
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_OLD");

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "ENC_OLD")).thenReturn(false);
    // 비밀번호 불일치 시 재검증 SSO도 실패하면 INVALID_PASSWORD 예외 기대
    when(schoolAuthClient.getStudentProfile(any(SsoRequest.class)))
        .thenThrow(new RuntimeException("sso fail"));

    // when
    BusinessException exception = assertThrows(BusinessException.class,
        () -> authService.login(studentNo, "wrong"));

    // then: 예외 코드 + 토큰 발급 미호출 검증
    assertSame(AuthErrorCode.INVALID_PASSWORD, exception.getErrorCode());
    verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString(), anyLong());
    System.out.println("exception = " + exception.getMessage());
  }

  @Test
  void 기존유저_정상재로그인시_기존RefreshToken_업데이트(){
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_OLD");

    RefreshToken existingToken = RefreshToken.builder()
            .user(user)
            .token("OLD_REFRESH_TOKEN")
            .expiryDate(Instant.parse("2026-04-01T00:00:00Z"))
            .build();

    //given
    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("pw", "ENC_OLD")).thenReturn(true);
    when(jwtUtil.generateToken(anyString(), eq(studentNo), anyString(),
            eq(jwtProperties.getAccessTokenExpiration()))).thenReturn("NEW_ACCESS_TOKEN");
    when(jwtUtil.generateToken(anyString(), eq(studentNo), anyString(),
            eq(jwtProperties.getRefreshTokenExpiration()))).thenReturn("NEW_REFRESH_TOKEN");
    //existingToken -> existingToken.updateToken(refreshToken, newExpiry) -> 해당 로직을 실행하기 위해서는 existingTokens을 반환값으로 줘야한다
    when(refreshTokenRepository.findByUser(any(User.class))).thenReturn(Optional.of(existingToken));

    //when
    LoginResponse response = authService.login(studentNo, "pw");

    //then
    //.createdAt(Instant.now(fixedClock))에서 fixedclock이 Now로 적용됐기 때문에, 실제로는 고정된 시간이 적용된 거다.
    // 그래서 동일하게 instant.now(fixedClock)으로 동일한 기준점을 명시해야한다
    Instant expectedExpiry = Instant.now(fixedClock).plusMillis(jwtProperties.getRefreshTokenExpiration());

    assertEquals("NEW_ACCESS_TOKEN", response.accessToken());
    assertEquals("NEW_REFRESH_TOKEN", existingToken.getToken());
    assertEquals(expectedExpiry, existingToken.getExpiryDate());
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    System.out.println("response.accessToken = " + response.accessToken());
    System.out.println("existingToken = " + existingToken.getToken());
    System.out.println("existingToken.getExpiryDate = " + existingToken.getExpiryDate());
    System.out.println("expectedExpiry = " + expectedExpiry);
  }

  @Test
  void 정지계정_정지기간안이면_USER_SUSPENDED() {
    // given: 정지 계정이며 정지 종료 시각이 아직 미래
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");
    user.updateStatus(UserStatus.SUSPENDED, Instant.now(fixedClock).plusSeconds(600));

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));
    // 비밀번호는 맞아도 상태 검사에서 차단되어야 한다.
    when(passwordEncoder.matches("pw", "ENC_PW")).thenReturn(true);

    // when
    BusinessException exception = assertThrows(BusinessException.class,
        () -> authService.login(studentNo, "pw"));

    // then: 정지 예외 + 토큰 미발급
    assertSame(AuthErrorCode.USER_SUSPENDED, exception.getErrorCode());
    verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString(), anyLong());
    System.out.println("exception = " + exception.getMessage());
  }

  @Test
  void 정지계정_정지기간지났으면_ACTIVE로복구후_로그인성공() {
    // given: 정지 계정이지만 정지 종료 시각이 이미 과거
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");
    user.updateStatus(UserStatus.SUSPENDED, Instant.now(fixedClock).minusSeconds(60));

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("pw", "ENC_PW")).thenReturn(true);
    // 복구 후 정상 로그인 시나리오 토큰 설정
    // login로직에서 access token 발급 로직이 호출되면, 진짜 JWT를 만들지 말고 "ACCESS_TOKEN" 문자열을 돌려줘라
    when(jwtUtil.generateToken(anyString(), eq(studentNo), anyString(),
        eq(jwtProperties.getAccessTokenExpiration()))).thenReturn("ACCESS_TOKEN");
    when(jwtUtil.generateToken(anyString(), eq(studentNo), anyString(),
        eq(jwtProperties.getRefreshTokenExpiration()))).thenReturn("REFRESH_TOKEN");
    when(refreshTokenRepository.findByUser(any(User.class))).thenReturn(Optional.empty());

    // when
    LoginResponse response = authService.login(studentNo, "pw");

    // then: 로그인 성공 + 상태가 ACTIVE로 자동 복구되었는지 검증
    assertEquals("ACCESS_TOKEN", response.accessToken());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
    assertEquals(User.activeDuration(), user.getDuration());
    System.out.println("response.studentNo = " + response.studentNo());
    System.out.println("user.getStatus = " + user.getStatus());
    System.out.println("user.getDuration() = " + user.getDuration());
  }



  //logout
  @Test
  void 로그아웃_성공_시_해당유저_refreshToken_삭제() {
    // given
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));

    // when
    authService.logout(studentNo);

    // then
    verify(refreshTokenRepository).deleteByUser(user);
  }

  @Test
  void 로그아웃_대상유저_없으면_USER_NOT_FOUND() {
    // given
    String studentNo = "99999999";
    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.empty());

    // when
    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.logout(studentNo)
    );

    // then
    assertSame(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
    System.out.println("exception = " + exception.getMessage());
  }

  @Test
  void 로그아웃_유저는있지만_refreshToken없어도_정상종료() {
    //given
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));

    // when & then
    assertDoesNotThrow(() -> authService.logout(studentNo));//refreshtoken이 없어도 로그아웃 로직은 정상 종료 되어야함. 예외 던지면 안됨
    verify(refreshTokenRepository).deleteByUser(user);
    // deleteByUser => 대상 row가 없어도 예외 없이 끝난다(0건 삭제).

  }


  //token 재발급
  @Test
  void refreshToken_유효하면_accessToken_재발급() {
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");
    RefreshToken storedToken = RefreshToken.builder()
            .user(user)
            .token("REFRESH_TOKEN")
            .expiryDate(Instant.now(fixedClock).plusSeconds(60))
            .build();

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(storedToken));
    when(jwtUtil.generateToken(
            eq(user.getId().toString()),
            eq(studentNo),
            eq(user.getRole().name()),
            eq(jwtProperties.getAccessTokenExpiration())
    )).thenReturn("NEW_ACCESS_TOKEN");

    RefreshResponse response = authService.accessRefresh(new RefreshRequest(studentNo));

    assertEquals(studentNo, response.studentNo());
    assertEquals("NEW_ACCESS_TOKEN", response.accessToken());
    assertEquals(jwtProperties.getAccessTokenExpiration(), response.expiresIn());
    verify(refreshTokenRepository, never()).deleteByUser(user);
  }

  @Test
  void 유저가_없으면_USER_NOT_FOUND() {
    String studentNo = "99999999";
    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(
            BusinessException.class,
            () -> authService.accessRefresh(new RefreshRequest(studentNo))
    );

    assertSame(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    System.out.println("exception = " + ex.getMessage());
  }

  @Test
  void refreshToken_테이블에_없으면_INVALID_TOKEN() {
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(
            BusinessException.class,
            () -> authService.accessRefresh(new RefreshRequest(studentNo))
    );

    assertSame(AuthErrorCode.INVALID_TOKEN, ex.getErrorCode());
    verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString(), anyLong());
    System.out.println("exception = " + ex.getMessage());
  }

  @Test
  void refreshToken_만료면_TOKEN_EXPIRED_그리고_토큰삭제(){
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");
    RefreshToken expiredToken = RefreshToken.builder()
            .user(user)
            .token("EXPIRED_TOKEN")
            .expiryDate(Instant.now(fixedClock).minusSeconds(1))
            .build();

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(expiredToken));

    //when
    BusinessException ex = assertThrows(
            BusinessException.class,
            () -> authService.accessRefresh(new RefreshRequest(studentNo))
    );

    //then
    assertSame(AuthErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
    verify(refreshTokenRepository).deleteByUser(user); // “코드 실행 중에 deleteByUser(user)라는 메서드를 최소 1번 호출했는가?” 확인
    verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString(), anyLong());
    System.out.println("exception = " + ex.getMessage());
  }


  //유저 정보 조회
  @Test
  void 프로필조회_성공() {
    // given
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));

    // when
    UserSearchResponse response = authService.getUserProfile(studentNo);

    // then
    assertEquals(user.getStudentNo(), response.studentNo());
    assertEquals(user.getName(), response.name());
    assertEquals(user.getRole(), response.role());
    assertEquals(user.getStatus(), response.status());
    System.out.println(response.toString());
  }

  @Test
  void 프로필조회_유저없으면_USER_NOT_FOUND(){
    //given
    String studentNo = "20201234";
    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.empty());

    //when
    BusinessException ex = assertThrows(
            BusinessException.class,
            () -> authService.getUserProfile(studentNo)
    );

    //then
    assertEquals(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    System.out.println("exception = " + ex.getMessage());
  }


  //유저 상태 변경 ***************************************



  //유저 상태 조기 복구
  @Test
  void 조기복구_성공시_ACTIVE와_EPOCH로_변경() {
    // given
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");
    user.updateStatus(UserStatus.SUSPENDED, Instant.now(fixedClock).plusSeconds(3600));

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));

    // when
    authService.activateStatus(studentNo);

    // then
    assertEquals(UserStatus.ACTIVE, user.getStatus());
    assertEquals(User.activeDuration(), user.getDuration());
    System.out.println(user.getStatus().toString());
    System.out.println(user.getDuration().toString());
  }

  @Test
  void 조기복구_유저없으면_USER_NOT_FOUND() {
    // given
    String studentNo = "99999999";
    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.empty());

    // when
    BusinessException ex = assertThrows(
            BusinessException.class,
            () -> authService.activateStatus(studentNo)
    );

    // then
    assertSame(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
    System.out.println("exception = " + ex.getMessage());
  }



  //유저 권한 변경
  @Test
  void 권한수정_성공() {
    // given
    String studentNo = "20201234";
    User user = buildUser(studentNo, "ENC_PW");

    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.of(user));

    // when
    authService.updateRole(studentNo, UserRole.ADMIN);

    // then
    assertEquals(UserRole.ADMIN, user.getRole());
    System.out.println(user.getRole().toString());
  }

  @Test
  void 권한수정_유저없으면_USER_NOT_FOUND() {
    // given
    String studentNo = "99999999";
    when(userRepository.findByStudentNo(studentNo)).thenReturn(Optional.empty());

    // when
    BusinessException ex = assertThrows(
            BusinessException.class,
            () -> authService.updateRole(studentNo, UserRole.SUPER)
    );

    // then
    assertSame(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
    System.out.println("exception = " + ex.getMessage());
  }

}
