package com.example.sejong_voice.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sejong_voice._common.security.UserPrincipalArgumentResolver;
import com.example.sejong_voice.comment.dto.CommentPageResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

  @Mock private CommentFacade commentFacade;

  private MockMvc mockMvc;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new CommentController(commentFacade))
            .setControllerAdvice(new GlobalExceptionHandler(fixedClock))
            .setCustomArgumentResolvers(
                new UserPrincipalArgumentResolver(), new PageableHandlerMethodArgumentResolver())
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void create_로그인사용자로_댓글을_생성한다() throws Exception {
    UUID postId = UUID.randomUUID();
    UUID rootCommentId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    authenticate();
    when(commentFacade.create(userId, postId, rootCommentId, "댓글")).thenReturn(commentId);

    mockMvc
        .perform(
            post("/api/v1/comments")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                {"postId":"%s","rootCommentId":"%s","content":"댓글"}
                """
                        .formatted(postId, rootCommentId)))
        .andExpect(status().isCreated())
        .andExpect(content().string("\"%s\"".formatted(commentId)));

    verify(commentFacade).create(userId, postId, rootCommentId, "댓글");
  }

  @Test
  void getAllByPostId_비로그인사용자는_null_userId로_조회한다() throws Exception {
    UUID postId = UUID.randomUUID();
    when(commentFacade.getAllByPostId(any(UUID.class), any(), any(Pageable.class)))
        .thenReturn(new CommentPageResponse(List.of(), 0, 20, 0, 0, 0, 0, true, true));

    mockMvc
        .perform(get("/api/v1/comments/post/{postId}", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0));

    ArgumentCaptor<UUID> postIdCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(commentFacade)
        .getAllByPostId(postIdCaptor.capture(), userIdCaptor.capture(), any(Pageable.class));
    assertEquals(postId, postIdCaptor.getValue());
    assertNull(userIdCaptor.getValue());
  }

  @Test
  void delete_로그인사용자로_댓글을_삭제한다() throws Exception {
    UUID commentId = UUID.randomUUID();
    authenticate();

    mockMvc.perform(delete("/api/v1/comments/{id}", commentId)).andExpect(status().isNoContent());

    verify(commentFacade).delete(userId, commentId);
  }

  private void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                TestFixtures.principal(userId, UserRole.STUDENT), "token", List.of()));
  }
}
