package com.example.sejong_voice.vote;

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
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.VoteChoice;
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
class VoteControllerTest {

  @Mock private VoteFacade voteFacade;

  private MockMvc mockMvc;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new VoteController(voteFacade))
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
  void cast_로그인사용자로_투표한다() throws Exception {
    UUID postId = UUID.randomUUID();
    authenticate();

    mockMvc
        .perform(
            put("/api/v1/posts/{postId}/votes", postId)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                {"choice":"AGREE"}
                """))
        .andExpect(status().isOk());

    verify(voteFacade).cast(postId, userId, VoteChoice.AGREE);
  }

  @Test
  void getSummary_응답을_반환한다() throws Exception {
    UUID postId = UUID.randomUUID();
    when(voteFacade.getSummary(postId)).thenReturn(new VoteSummaryResponse(7, 3, 10));

    mockMvc
        .perform(get("/api/v1/posts/{postId}/votes/summary", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.agreeCount").value(7))
        .andExpect(jsonPath("$.disagreeCount").value(3))
        .andExpect(jsonPath("$.totalCount").value(10));
  }

  @Test
  void getStatistics_로그인사용자_id로_조회한다() throws Exception {
    UUID postId = UUID.randomUUID();
    authenticate();
    when(voteFacade.getStatistics(postId, userId))
        .thenReturn(
            new com.example.sejong_voice.vote.dto.VoteStatisticsResponse(
                7, 3, 10, new java.math.BigDecimal("0.7000"), List.of()));

    mockMvc
        .perform(get("/api/v1/posts/{postId}/votes/statistics", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.agreeCount").value(7));

    verify(voteFacade).getStatistics(postId, userId);
  }

  @Test
  void getAgreeVoters_로그인사용자_id로_조회한다() throws Exception {
    UUID postId = UUID.randomUUID();
    authenticate();
    when(voteFacade.getAgreeVoters(postId, userId)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/posts/{postId}/votes/agree-voters", postId))
        .andExpect(status().isOk());

    verify(voteFacade).getAgreeVoters(postId, userId);
  }

  private void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                TestFixtures.principal(userId, UserRole.STUDENT), "token", List.of()));
  }
}
