package com.example.sejong_voice.post;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.example.sejong_voice.exception.GlobalExceptionHandler;
import com.example.sejong_voice.post.dto.PostDetailResponse;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.UserRole;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

  @Mock private PostFacade postFacade;

  private MockMvc mockMvc;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PostController(postFacade))
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
  void create_로그인사용자로_게시글을_생성한다() throws Exception {
    UUID postId = UUID.randomUUID();
    UUID councilId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    authenticate();
    when(postFacade.create(
            userId,
            "제목",
            "본문",
            councilId,
            categoryId,
            PostVotingDuration.ONE_WEEK,
            List.of(imageId)))
        .thenReturn(postId);

    mockMvc
        .perform(
            post("/api/v1/posts")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                {"title":"제목","content":"본문","councilId":"%s","categoryId":"%s","postVotingDuration":"ONE_WEEK","imageIds":["%s"]}
                """
                        .formatted(councilId, categoryId, imageId)))
        .andExpect(status().isCreated())
        .andExpect(content().string("\"%s\"".formatted(postId)));
  }

  @Test
  void getPosts_비로그인사용자는_null_userId로_조회한다() {
    PostController controller = new PostController(postFacade);
    PageImpl<PostListResponse> page = new PageImpl<>(List.of());
    when(postFacade.getPosts(
            any(com.example.sejong_voice.post.dto.PostSearchCondition.class), any(Pageable.class)))
        .thenReturn(page);

    ResponseEntity<org.springframework.data.domain.Page<PostListResponse>> response =
        controller.getPosts(
            new com.example.sejong_voice.post.dto.PostSearchCondition(
                null, null, null, null, null, null, null),
            PageRequest.of(0, 10),
            null);

    ArgumentCaptor<com.example.sejong_voice.post.dto.PostSearchCondition> conditionCaptor =
        ArgumentCaptor.forClass(com.example.sejong_voice.post.dto.PostSearchCondition.class);
    verify(postFacade).getPosts(conditionCaptor.capture(), any(Pageable.class));
    assertSame(page, response.getBody());
    assertNull(conditionCaptor.getValue().currentUserId());
  }

  @Test
  void get_로그인사용자_id로_상세조회한다() throws Exception {
    UUID postId = UUID.randomUUID();
    authenticate();
    when(postFacade.get(postId, userId))
        .thenReturn(
            PostDetailResponse.builder()
                .id(postId)
                .title("제목")
                .status(PostStatus.VOTING)
                .myVoteChoice(VoteChoice.AGREE)
                .canVote(true)
                .canCloseEarly(false)
                .images(List.of())
                .statements(List.of())
                .build());

    mockMvc
        .perform(get("/api/v1/posts/{postId}", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(postId.toString()))
        .andExpect(jsonPath("$.title").value("제목"))
        .andExpect(jsonPath("$.myVoteChoice").value("AGREE"));
  }

  @Test
  void delete_로그인사용자로_삭제한다() throws Exception {
    UUID postId = UUID.randomUUID();
    authenticate();

    mockMvc.perform(delete("/api/v1/posts/{postId}", postId)).andExpect(status().isNoContent());

    verify(postFacade).delete(postId, userId);
  }

  @Test
  void closeEarly_로그인사용자로_조기종료한다() throws Exception {
    UUID postId = UUID.randomUUID();
    authenticate();

    mockMvc.perform(post("/api/v1/posts/{postId}/close", postId)).andExpect(status().isOk());

    verify(postFacade).closeEarly(postId, userId);
  }

  @Test
  void createStatement_로그인사용자로_입장문을_생성한다() throws Exception {
    UUID postId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    authenticate();

    mockMvc
        .perform(
            post("/api/v1/posts/{postId}/statements", postId)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                {"finalStatus":"COMPLETED","content":"입장문","imageIds":["%s"]}
                """
                        .formatted(imageId)))
        .andExpect(status().isCreated());

    verify(postFacade)
        .createStatement(postId, userId, PostStatus.COMPLETED, "입장문", List.of(imageId));
  }

  private void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                TestFixtures.principal(userId, UserRole.STUDENT), "token", List.of()));
  }
}
