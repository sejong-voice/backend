package com.example.sejong_voice.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.entity.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  @Mock private PostRepository postRepository;

  private PostService postService;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    postService = new PostService(postRepository, fixedClock);
  }

  @Test
  void create_게시글을_생성하고_스냅샷과_마감시각을_저장한다() {
    User user = buildUser(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    Council council =
        buildCouncil(
            UUID.fromString("00000000-0000-0000-0000-000000000010"),
            30,
            new BigDecimal("0.6667"),
            user);
    Category category = buildCategory(UUID.fromString("00000000-0000-0000-0000-000000000020"));

    when(postRepository.save(any(Post.class)))
        .thenAnswer(
            invocation -> {
              Post post = invocation.getArgument(0);
              ReflectionTestUtils.setField(
                  post, "id", UUID.fromString("00000000-0000-0000-0000-000000000100"));
              return post;
            });

    Post created =
        postService.create(user, council, category, "등록금 인상 반대", "본문", PostVotingDuration.ONE_WEEK);

    assertEquals(user, created.getUser());
    assertEquals(council, created.getCouncil());
    assertEquals(category, created.getCategory());
    assertEquals(PostStatus.VOTING, created.getStatus());
    assertEquals(30, created.getMinVoteSnapshot());
    assertEquals(new BigDecimal("0.6667"), created.getApproveRatioSnapshot());
    assertEquals(Instant.now(fixedClock), created.getCreatedAt());
    assertEquals(Instant.now(fixedClock), created.getVotingStartAt());
    assertNotNull(created.getVotingEndAt());
    assertEquals(1, created.getViewCount());
    verify(postRepository).save(any(Post.class));
  }

  @Test
  void get_게시글이_없으면_POST_NOT_FOUND() {
    UUID postId = UUID.randomUUID();
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> postService.get(postId));

    assertSame(PostErrorCode.POST_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void getByIdForUpdate_게시글이_없으면_POST_NOT_FOUND() {
    UUID postId = UUID.randomUUID();
    when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> postService.getByIdForUpdate(postId));

    assertSame(PostErrorCode.POST_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void getPosts_검색결과를_그대로_반환한다() {
    PostSearchCondition condition =
        new PostSearchCondition(null, null, null, "등록금", null, null, null);
    Pageable pageable = PageRequest.of(0, 10);
    PostListResponse response =
        PostListResponse.builder().id(UUID.randomUUID()).title("등록금 인상 반대").build();
    Page<PostListResponse> expected = new PageImpl<>(List.of(response), pageable, 1);

    when(postRepository.search(condition, pageable)).thenReturn(expected);

    Page<PostListResponse> result = postService.getPosts(condition, pageable);

    assertSame(expected, result);
  }

  @Test
  void delete_삭제시각을_기록한다() {
    UUID postId = UUID.randomUUID();
    Post post = buildPost(PostStatus.VOTING, 10, new BigDecimal("0.5000"));
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));

    postService.delete(postId);

    assertEquals(Instant.now(fixedClock), post.getDeletedAt());
  }

  @Test
  void close_승인조건을_충족하면_APPROVED로_변경한다() {
    UUID postId = UUID.randomUUID();
    Post post = buildPost(PostStatus.VOTING, 10, new BigDecimal("0.6000"));
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));

    postService.close(postId, 12, 20);

    assertEquals(Instant.now(fixedClock), post.getClosedAt());
    assertEquals(PostStatus.APPROVED, post.getStatus());
  }

  @Test
  void close_최소투표수_미달이면_PENDING으로_변경한다() {
    UUID postId = UUID.randomUUID();
    Post post = buildPost(PostStatus.VOTING, 30, new BigDecimal("0.6000"));
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));

    postService.close(postId, 20, 25);

    assertEquals(PostStatus.PENDING, post.getStatus());
    assertEquals(Instant.now(fixedClock), post.getClosedAt());
  }

  @Test
  void close_이미_투표종료된_게시글이면_아무것도_변경하지_않는다() {
    UUID postId = UUID.randomUUID();
    Post post = buildPost(PostStatus.APPROVED, 10, new BigDecimal("0.5000"));
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));

    postService.close(postId, 10, 10);

    assertEquals(PostStatus.APPROVED, post.getStatus());
    assertNull(post.getClosedAt());
  }

  @Test
  void closeEarly_승인조건을_충족하면_즉시_APPROVED로_변경한다() {
    UUID postId = UUID.randomUUID();
    Post post = buildPost(PostStatus.VOTING, 10, new BigDecimal("0.6000"));
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));

    postService.closeEarly(postId, 12, 20);

    assertEquals(PostStatus.APPROVED, post.getStatus());
    assertEquals(Instant.now(fixedClock), post.getClosedAt());
  }

  @Test
  void closeEarly_승인조건을_충족하지_못하면_예외를_던진다() {
    UUID postId = UUID.randomUUID();
    Post post = buildPost(PostStatus.VOTING, 10, new BigDecimal("0.8000"));
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> postService.closeEarly(postId, 7, 10));

    assertSame(PostErrorCode.EARLY_CLOSE_NOT_AVAILABLE, exception.getErrorCode());
    assertNull(post.getClosedAt());
    assertEquals(PostStatus.VOTING, post.getStatus());
  }

  @Test
  void canCloseEarly_투표중이고_최소투표수와_찬성비율을_충족하면_true() {
    Post post = buildPost(PostStatus.VOTING, 10, new BigDecimal("0.7000"));

    boolean result = postService.canCloseEarly(post, 14, 20);

    assertTrue(result);
  }

  @Test
  void canCloseEarly_총투표수가_0이면_false() {
    Post post = buildPost(PostStatus.VOTING, 0, new BigDecimal("0.5000"));

    boolean result = postService.canCloseEarly(post, 0, 0);

    assertFalse(result);
  }

  @Test
  void findClosable_리포지토리_조회결과를_반환한다() {
    Instant now = Instant.now(fixedClock);
    Post post = buildPost(PostStatus.VOTING, 10, new BigDecimal("0.5000"));
    when(postRepository.findAllByStatusAndVotingEndAtBefore(PostStatus.VOTING, now))
        .thenReturn(List.of(post));

    List<Post> result = postService.findClosable(PostStatus.VOTING, now);

    assertEquals(1, result.size());
    assertSame(post, result.getFirst());
    verify(postRepository).findAllByStatusAndVotingEndAtBefore(PostStatus.VOTING, now);
  }

  private User buildUser(UUID id) {
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

    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private Council buildCouncil(
      UUID id, int minVote, BigDecimal approveRatioThreshold, User adminUser) {
    Affiliation affiliation = Affiliation.builder().name("총학생회").build();

    Council council =
        Council.builder()
            .affiliation(affiliation)
            .adminUser(adminUser)
            .minVote(minVote)
            .approveRatioThreshold(approveRatioThreshold)
            .updatedAt(Instant.now(fixedClock))
            .build();

    ReflectionTestUtils.setField(council, "id", id);
    return council;
  }

  private Category buildCategory(UUID id) {
    Category category = Category.builder().name("학사").displayOrder(1).build();

    ReflectionTestUtils.setField(category, "id", id);
    return category;
  }

  private Post buildPost(PostStatus status, int minVoteSnapshot, BigDecimal approveRatioSnapshot) {
    User user = buildUser(UUID.randomUUID());
    Council council = buildCouncil(UUID.randomUUID(), minVoteSnapshot, approveRatioSnapshot, user);
    Category category = buildCategory(UUID.randomUUID());

    Post post =
        Post.builder()
            .user(user)
            .council(council)
            .category(category)
            .title("등록금 인상 반대")
            .content("본문")
            .postVotingDuration(PostVotingDuration.ONE_WEEK)
            .minVoteSnapshot(minVoteSnapshot)
            .approveRatioSnapshot(approveRatioSnapshot)
            .createdAt(Instant.now(fixedClock))
            .build();

    ReflectionTestUtils.setField(post, "id", UUID.randomUUID());
    if (status != PostStatus.VOTING) {
      post.updateStatus(status);
    }

    return post;
  }
}
