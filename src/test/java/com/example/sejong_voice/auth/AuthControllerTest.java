package com.example.sejong_voice.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice._common.security.UserPrincipalArgumentResolver;
import com.example.sejong_voice.auth.dto.LoginResponse;
import com.example.sejong_voice.auth.dto.RefreshResponse;
import com.example.sejong_voice.auth.dto.UserSearchResponse;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.exception.GlobalExceptionHandler;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthService authService;

  private MockMvc mockMvc;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-04-21T00:00:00Z"), ZoneOffset.UTC);

    AuthController controller = new AuthController(authService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(fixedClock))
            .setCustomArgumentResolvers(new UserPrincipalArgumentResolver())
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();

    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void login_성공_200_응답매핑_서비스호출검증() throws Exception {
    when(authService.login("20201234", "pw"))
        .thenReturn(new LoginResponse("20201234", "ACCESS_TOKEN", 3600));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                {"studentNo":"20201234","password":"pw"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentNo").value("20201234"))
        .andExpect(jsonPath("$.accessToken").value("ACCESS_TOKEN"))
        .andExpect(jsonPath("$.expiresIn").value(3600));

    verify(authService).login("20201234", "pw");
  }

  @Test
  void refresh_성공_200_응답매핑_서비스호출검증() throws Exception {
    when(authService.accessRefresh(any()))
        .thenReturn(new RefreshResponse("20201234", "NEW_ACCESS_TOKEN", 3600));

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                {"studentNo":"20201234"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentNo").value("20201234"))
        .andExpect(jsonPath("$.accessToken").value("NEW_ACCESS_TOKEN"))
        .andExpect(jsonPath("$.expiresIn").value(3600));

    verify(authService).accessRefresh(any());
  }

  @Test
  void profile_정상principal_주입되면_200_서비스호출검증() throws Exception {
    setAuthenticatedPrincipal("20201234");
    when(authService.getUserProfile("20201234"))
        .thenReturn(
            new UserSearchResponse(
                UUID.randomUUID(),
                "20201234",
                "홍길동",
                "컴퓨터공학과",
                UserRole.STUDENT,
                UserStatus.ACTIVE));

    mockMvc
        .perform(get("/api/v1/auth/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentNo").value("20201234"))
        .andExpect(jsonPath("$.name").value("홍길동"));

    verify(authService).getUserProfile("20201234");
  }

  @Test
  void profile_principal없으면_INVALID_USER_401() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/profile"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_008"))
        .andExpect(jsonPath("$.message").value(AuthErrorCode.INVALID_USER.getMessage()));

    verify(authService, never()).getUserProfile(anyString());
  }

  @Test
  void logout_정상principal이면_204_서비스호출검증() throws Exception {
    setAuthenticatedPrincipal("20201234");

    mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());

    verify(authService).logout("20201234");
  }

  @Test
  void logout_principal없으면_INVALID_USER_401() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/logout"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_008"));

    verify(authService, never()).logout(anyString());
  }

  @Test
  void 서비스에서_BusinessException_던지면_GlobalExceptionHandler로_매핑() throws Exception {
    setAuthenticatedPrincipal("20201234");
    when(authService.getUserProfile("20201234"))
        .thenThrow(new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    mockMvc
        .perform(get("/api/v1/auth/profile"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("AUTH_003"))
        .andExpect(jsonPath("$.message").value(AuthErrorCode.USER_NOT_FOUND.getMessage()));
  }

  private void setAuthenticatedPrincipal(String studentNo) {
    UserPrincipal principal =
        UserPrincipal.builder()
            .userId(UUID.randomUUID())
            .studentNo(studentNo)
            .role(UserRole.STUDENT)
            .build();

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token", List.of()));
  }
}
