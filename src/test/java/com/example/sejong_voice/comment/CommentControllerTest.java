package com.example.sejong_voice.comment;

import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import com.example.sejong_voice.comment.dto.CommentCreateRequest;
import com.example.sejong_voice.comment.dto.CommentResponse;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.user.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CommentService commentService;

  @MockitoBean
  private JwtUtil jwtUtil; // SecurityConfig에서 필요

  private UserPrincipal userPrincipal;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    userPrincipal = UserPrincipal.builder()
        .userId(userId)
        .studentNo("20260001")
        .role(UserRole.STUDENT)
        .build();

    // SecurityContext에 인증 정보 설정
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userPrincipal, null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  @DisplayName("댓글 생성 API 호출 성공")
  void createComment_Success() throws Exception {
    // given
    UUID postId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentCreateRequest request = new CommentCreateRequest(postId, null, "댓글 내용");

    given(commentService.create(eq(userId), eq(postId), eq(null), any()))
        .willReturn(commentId);

    // when & then
    mockMvc.perform(post("/api/v1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(content().string("\"" + commentId + "\""));
  }

  @Test
  @DisplayName("게시글별 댓글 조회 API 호출 성공")
  void getCommentsByPost_Success() throws Exception {
    // given
    UUID postId = UUID.randomUUID();
    CommentResponse response = new CommentResponse(
        UUID.randomUUID(),
        "작성자",
        "댓글 내용",
        CommentStatus.ACTIVE,
        List.of(),
        LocalDateTime.now()
    );

    given(commentService.getByPostId(postId)).willReturn(List.of(response));

    // when & then
    mockMvc.perform(get("/api/v1/comments/post/{postId}", postId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].userName").value("작성자"))
        .andExpect(jsonPath("$[0].content").value("댓글 내용"));
  }

  @Test
  @DisplayName("댓글 삭제 API 호출 성공")
  void deleteComment_Success() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId))
        .andExpect(status().isNoContent());
  }
}
