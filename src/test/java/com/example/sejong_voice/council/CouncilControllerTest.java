package com.example.sejong_voice.council;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sejong_voice._common.security.UserPrincipalArgumentResolver;
import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.exception.GlobalExceptionHandler;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.UserRole;
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
class CouncilControllerTest {

  @Mock private CouncilFacade councilFacade;

  private MockMvc mockMvc;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new CouncilController(councilFacade))
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
  void getAll_비로그인사용자는_null_userId로_조회한다() throws Exception {
    when(councilFacade.getCouncils("총", true, null))
        .thenReturn(List.of(new CouncilResponse(UUID.randomUUID(), "총학생회")));

    mockMvc
        .perform(get("/api/v1/councils").param("keyword", "총").param("accessible", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("총학생회"));

    verify(councilFacade).getCouncils("총", true, null);
  }

  @Test
  void getAll_로그인사용자는_principal_userId를_전달한다() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                TestFixtures.principal(userId, UserRole.STUDENT), "token", List.of()));
    when(councilFacade.getCouncils(null, false, userId))
        .thenReturn(List.of(new CouncilResponse(UUID.randomUUID(), "공과대학")));

    mockMvc
        .perform(get("/api/v1/councils").param("accessible", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("공과대학"));

    verify(councilFacade).getCouncils(null, false, userId);
  }
}
