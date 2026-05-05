package com.example.sejong_voice.council.policy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sejong_voice._common.security.UserPrincipalArgumentResolver;
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
class PolicyControllerTest {

  @Mock private PolicyFacade policyFacade;

  private MockMvc mockMvc;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PolicyController(policyFacade))
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
  void get_정책응답을_반환한다() throws Exception {
    UUID councilId = UUID.randomUUID();
    when(policyFacade.getPolicy(councilId))
        .thenReturn(
            new com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse(
                10, new java.math.BigDecimal("0.6000")));

    mockMvc
        .perform(get("/api/v1/councils/{councilId}/policies", councilId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.minVote").value(10))
        .andExpect(jsonPath("$.approveRatioThreshold").value(0.6000));
  }

  @Test
  void update_로그인사용자_id로_수정한다() throws Exception {
    UUID councilId = UUID.randomUUID();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                TestFixtures.principal(userId, UserRole.STUDENT), "token", List.of()));

    mockMvc
        .perform(
            put("/api/v1/councils/{councilId}/policies", councilId)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                {"minVote":15,"approveRatioThreshold":0.7}
                """))
        .andExpect(status().isOk());

    verify(policyFacade).updatePolicy(councilId, userId, 15, new java.math.BigDecimal("0.7"));
  }
}
