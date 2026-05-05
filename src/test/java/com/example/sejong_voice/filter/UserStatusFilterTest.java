package com.example.sejong_voice.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice._config.filter.UserStatusFilter;
import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserStatusFilterTest {
  @Mock private UserRepository userRepository;

  @Mock private FilterChain filterChain;

  private ObjectMapper objectMapper;
  private Clock fixedClock;
  private UserStatusFilter filter;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    fixedClock = Clock.fixed(Instant.parse("2026-04-18T00:00:00Z"), ZoneOffset.UTC);
    filter = new UserStatusFilter(userRepository, objectMapper, fixedClock);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 인증없으면_체인통과() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    verify(filterChain).doFilter(req, res);
    verify(userRepository, never()).findById(any());
  }

  @Test
  void principal이_UserPrincipal아니면_체인통과() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse res = new MockHttpServletResponse();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("string-principal", "token")
            // principal대신에 "string-principal"이 들어가서 유저 인증 객체 대신 잘못된 객체가 들어감
            // 다만 Authentication이 UserPrincipal일 때만 DB 상태 검사
            );

    filter.doFilter(req, res, filterChain);

    verify(filterChain).doFilter(req, res);
    verify(userRepository, never()).findById(any());
  }

  @Test
  void ACTIVE유저면_체인통과() throws Exception {
    UUID userId = UUID.randomUUID();
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse res = new MockHttpServletResponse();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().userId(userId).studentNo("20201234").build(),
                "token",
                List.of()));

    User user = buildUser(userId);
    ReflectionTestUtils.setField(user, "id", userId);
    user.updateStatus(UserStatus.ACTIVE, User.activeDuration());

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // when
    filter.doFilter(req, res, filterChain);

    // then
    verify(filterChain).doFilter(req, res);
    verify(userRepository).findById(any());
  }

  @ParameterizedTest
  @EnumSource(
      value = UserStatus.class,
      names = {"SUSPENDED", "WITHDRAWN"})
  void SUSPENDED_또는_WITHDRAWN이면_403에러바디_체인중단(UserStatus blockedStatus) throws Exception {
    UUID userId = UUID.randomUUID();
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse res = new MockHttpServletResponse();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().userId(userId).studentNo("20201234").build(),
                "token",
                List.of()));
    User user = buildUser(userId);
    ReflectionTestUtils.setField(user, "id", userId);
    user.updateStatus(blockedStatus, User.activeDuration()); // 핵심 수정

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // when
    filter.doFilter(req, res, filterChain);

    // then
    verify(userRepository).findById(userId);
    // 에러 코드, 타입, utf셋팅 값 등 접근차단 로직과 상태 비교
    assertNull(SecurityContextHolder.getContext().getAuthentication());

    MediaType contentType = MediaType.parseMediaType(res.getContentType());
    assertEquals(MediaType.APPLICATION_JSON.getType(), contentType.getType());
    assertEquals(MediaType.APPLICATION_JSON.getSubtype(), contentType.getSubtype());
    assertEquals(StandardCharsets.UTF_8, contentType.getCharset());

    // 에러 응답 포맷/ 내용까지 비교
    JsonNode body = objectMapper.readTree(res.getContentAsString());
    assertEquals("AUTH_002", body.get("code").asText());
    assertEquals(403, body.get("status").asInt());
    assertEquals(AuthErrorCode.USER_SUSPENDED.getMessage(), body.get("message").asText());

    // 다음 필터로 안 넘어가고 차단되는지
    verify(filterChain, never()).doFilter(req, res);
  }

  @Test
  void DB유저미존재면_USER_NOT_FOUND_예외() throws Exception {
    UUID userId = UUID.randomUUID();
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse res = new MockHttpServletResponse();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().userId(userId).studentNo("20201234").build(),
                "token",
                List.of()));

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when
    BusinessException ex =
        assertThrows(BusinessException.class, () -> filter.doFilter(req, res, filterChain));

    // then
    assertEquals(AuthErrorCode.USER_NOT_FOUND.getMessage(), ex.getMessage());
    verify(filterChain, never()).doFilter(req, res);
  }

  private User buildUser(UUID userId) {
    Affiliation affiliation = Affiliation.builder().name("컴퓨터공학과").build();

    User user =
        User.builder()
            .studentNo("20201234")
            .name("홍길동")
            .affiliation(affiliation)
            .password("ENC_PW")
            .duration(User.activeDuration())
            .createdAt(Instant.now(fixedClock))
            .build();

    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
