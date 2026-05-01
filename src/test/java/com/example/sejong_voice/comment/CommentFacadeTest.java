package com.example.sejong_voice.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.comment.dto.CommentPageResponse;
import com.example.sejong_voice.post.PostService;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CommentFacadeTest {

  @Mock private CommentService commentService;
  @Mock private UserService userService;
  @Mock private PostService postService;

  private CommentFacade commentFacade;
  private Instant now;

  @BeforeEach
  void setUp() {
    commentFacade = new CommentFacade(commentService, userService, postService);
    now = Instant.parse("2026-05-01T00:00:00Z");
  }

  @Test
  void create_유저와_게시글을_조회한_뒤_댓글생성을_위임한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    UUID rootCommentId = UUID.randomUUID();
    User user = buildUser(userId);
    Post post = buildPost(postId, user);
    UUID commentId = UUID.randomUUID();
    when(userService.getById(userId)).thenReturn(user);
    when(postService.get(postId)).thenReturn(post);
    when(commentService.create(user, post, rootCommentId, "댓글")).thenReturn(commentId);

    UUID result = commentFacade.create(userId, postId, rootCommentId, "댓글");

    assertEquals(commentId, result);
  }

  @Test
  void getAllByPostId_게시글을_조회한_뒤_댓글페이지를_반환한다() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID());
    Post post = buildPost(postId, author);
    CommentPageResponse response =
        new CommentPageResponse(List.of(), 0, 20, 0, 0, 0, 0, true, true);
    when(postService.get(postId)).thenReturn(post);
    when(commentService.getAllByPost(post, userId, PageRequest.of(0, 20))).thenReturn(response);

    CommentPageResponse result =
        commentFacade.getAllByPostId(postId, userId, PageRequest.of(0, 20));

    assertSame(response, result);
  }

  @Test
  void delete_서비스에_위임한다() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    commentFacade.delete(userId, commentId);

    verify(commentService).delete(userId, commentId);
  }

  private User buildUser(UUID id) {
    return TestFixtures.user(
        id,
        "20201234",
        "홍길동",
        TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
        UserRole.STUDENT,
        now);
  }

  private Post buildPost(UUID id, User author) {
    return TestFixtures.post(
        id,
        author,
        TestFixtures.council(
            UUID.randomUUID(),
            TestFixtures.affiliation(UUID.randomUUID(), "총학생회"),
            author,
            10,
            new BigDecimal("0.5000"),
            now),
        TestFixtures.category(UUID.randomUUID(), "학사"),
        PostStatus.VOTING,
        10,
        new BigDecimal("0.5000"),
        now);
  }
}
