package com.example.sejong_voice.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.category.CategoryService;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.comment.CommentService;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.ImageService;
import com.example.sejong_voice.image.dto.PostImageView;
import com.example.sejong_voice.notification.NotificationService;
import com.example.sejong_voice.post.dto.PostDetailResponse;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.vote.VoteService;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
class PostFacadeTest {

  @Mock private PostService postService;
  @Mock private PostStatementService postStatementService;
  @Mock private CategoryService categoryService;
  @Mock private UserService userService;
  @Mock private CouncilService councilService;
  @Mock private VoteService voteService;
  @Mock private NotificationService notificationService;
  @Mock private ImageService imageService;
  @Mock private CommentService commentService;
  @Mock private PostViewCountService postViewCountService;

  private PostFacade postFacade;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    postFacade =
        new PostFacade(
            postService,
            postStatementService,
            categoryService,
            userService,
            councilService,
            voteService,
            notificationService,
            imageService,
            commentService,
            postViewCountService,
            fixedClock);
  }

  @Test
  void create_접근가능한_의회가_아니면_ACCESS_DENIED() {
    UUID userId = UUID.randomUUID();
    UUID councilId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    User user = buildUser(userId, UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(councilId, 10, new BigDecimal("0.5000"), user, "총학생회");

    when(userService.getById(userId)).thenReturn(user);
    when(councilService.getById(councilId)).thenReturn(council);
    when(councilService.hasAccessToCouncil(user, council)).thenReturn(false);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                postFacade.create(
                    userId,
                    "제목",
                    "본문",
                    councilId,
                    categoryId,
                    PostVotingDuration.ONE_WEEK,
                    List.of()));

    assertSame(PostErrorCode.ACCESS_DENIED, exception.getErrorCode());
    verify(postService, never()).create(any(), any(), any(), any(), any(), any());
  }

  @Test
  void create_성공하면_게시글생성후_이미지를_연결하고_ID를_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID councilId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    List<UUID> imageIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    User user = buildUser(userId, UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(councilId, 10, new BigDecimal("0.5000"), user, "총학생회");
    Category category = buildCategory(categoryId, "학사");
    Post post = buildPost(UUID.randomUUID(), user, council, category, PostStatus.VOTING);

    when(userService.getById(userId)).thenReturn(user);
    when(councilService.getById(councilId)).thenReturn(council);
    when(councilService.hasAccessToCouncil(user, council)).thenReturn(true);
    when(categoryService.getById(categoryId)).thenReturn(category);
    when(postService.create(user, council, category, "제목", "본문", PostVotingDuration.ONE_WEEK))
        .thenReturn(post);

    UUID result =
        postFacade.create(
            userId, "제목", "본문", councilId, categoryId, PostVotingDuration.ONE_WEEK, imageIds);

    assertEquals(post.getId(), result);
    verify(imageService).attachImagesToPost(user, post, imageIds);
  }

  @Test
  void getPosts_assignedToMe인데_관리의회가_없으면_ACCESS_DENIED() {
    UUID currentUserId = UUID.randomUUID();
    PostSearchCondition condition =
        new PostSearchCondition(null, null, null, null, null, true, currentUserId);
    Pageable pageable = PageRequest.of(0, 10);

    when(councilService.hasManagedCouncil(currentUserId)).thenReturn(false);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> postFacade.getPosts(condition, pageable));

    assertSame(PostErrorCode.ACCESS_DENIED, exception.getErrorCode());
    verify(postService, never()).getPosts(any(), any());
  }

  @Test
  void getPosts_성공하면_투표수와_댓글수를_합쳐서_반환한다() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID councilId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    PostSearchCondition condition =
        new PostSearchCondition(null, null, null, null, null, null, null);
    Pageable pageable = PageRequest.of(0, 10);
    PostListResponse response =
        PostListResponse.builder()
            .id(postId)
            .userId(userId)
            .councilId(councilId)
            .councilName("총학생회")
            .categoryId(categoryId)
            .categoryName("학사")
            .title("제목")
            .content("본문")
            .status(PostStatus.VOTING)
            .postVotingDuration(PostVotingDuration.ONE_WEEK)
            .voteCount(7L)
            .commentCount(3L)
            .createdAt(Instant.now(fixedClock))
            .votingEndAt(Instant.now(fixedClock).plusSeconds(60))
            .build();
    Page<PostListResponse> page = new PageImpl<>(List.of(response), pageable, 1);

    when(postService.getPosts(condition, pageable)).thenReturn(page);

    Page<PostListResponse> result = postFacade.getPosts(condition, pageable);

    PostListResponse enriched = result.getContent().getFirst();
    assertEquals(7L, enriched.voteCount());
    assertEquals(3L, enriched.commentCount());
  }

  @Test
  void delete_작성자도_슈퍼관리자도_의회관리자도_아니면_ACCESS_DENIED() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    User actor = buildUser(userId, UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);

    when(postService.get(postId)).thenReturn(post);
    when(userService.getById(userId)).thenReturn(actor);
    when(councilService.isAdmin(userId, council.getId())).thenReturn(false);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> postFacade.delete(postId, userId));

    assertSame(PostErrorCode.ACCESS_DENIED, exception.getErrorCode());
    verify(postService, never()).delete(any());
  }

  @Test
  void delete_슈퍼관리자가_아니고_투표종료된_게시글이면_삭제불가() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(userId, UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.APPROVED);

    when(postService.get(postId)).thenReturn(post);
    when(userService.getById(userId)).thenReturn(author);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> postFacade.delete(postId, userId));

    assertSame(PostErrorCode.POST_CANNOT_DELETE_VOTING_ENDED, exception.getErrorCode());
    verify(commentService, never()).deleteAllByPostId(any());
    verify(imageService, never()).deleteAllByPostId(any());
  }

  @Test
  void delete_작성자가_투표중인_게시글을_삭제하면_연관데이터도_삭제한다() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(userId, UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);

    when(postService.get(postId)).thenReturn(post);
    when(userService.getById(userId)).thenReturn(author);
    when(councilService.isAdmin(userId, council.getId())).thenReturn(false);

    postFacade.delete(postId, userId);

    verify(commentService).deleteAllByPostId(postId);
    verify(imageService).deleteAllByPostId(postId);
    verify(postService).delete(postId);
  }

  @Test
  void closeEarly_의회관리자가_아니면_ACCESS_DENIED() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);

    when(postService.get(postId)).thenReturn(post);
    when(councilService.isAdmin(userId, council.getId())).thenReturn(false);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> postFacade.closeEarly(postId, userId));

    assertSame(PostErrorCode.ACCESS_DENIED, exception.getErrorCode());
    verify(notificationService, never()).createPostApprovedNotifications(any());
  }

  @Test
  void closeEarly_성공하면_조기종료후_알림을_생성한다() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);

    when(postService.get(postId)).thenReturn(post);
    when(councilService.isAdmin(userId, council.getId())).thenReturn(true);
    when(voteService.countAgree(postId)).thenReturn(12L);
    when(voteService.countDisagree(postId)).thenReturn(8L);

    postFacade.closeEarly(postId, userId);

    verify(postService).closeEarly(postId, 12L, 20L);
    verify(notificationService).createPostApprovedNotifications(post);
  }

  @Test
  void createStatement_첫입장문인데_최종상태가_없으면_FINAL_STATUS_REQUIRED() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.APPROVED);

    when(postService.getByIdForUpdate(postId)).thenReturn(post);
    when(councilService.isAdmin(userId, council.getId())).thenReturn(true);
    when(postStatementService.countStatements(postId)).thenReturn(0L);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> postFacade.createStatement(postId, userId, null, "입장문", List.of()));

    assertSame(PostErrorCode.FINAL_STATUS_REQUIRED, exception.getErrorCode());
    verify(postStatementService, never()).create(any(), any(), eq(1), any());
  }

  @Test
  void createStatement_첫입장문_성공시_게시글상태를_최종상태로_변경하고_이미지를_연결한다() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    List<UUID> imageIds = List.of(UUID.randomUUID());
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    User admin = buildUser(userId, UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), admin, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.APPROVED);
    PostStatement statement =
        PostStatement.builder()
            .post(post)
            .createdBy(admin)
            .sequence(1)
            .content("입장문")
            .createdAt(Instant.now(fixedClock))
            .build();

    when(postService.getByIdForUpdate(postId)).thenReturn(post);
    when(councilService.isAdmin(userId, council.getId())).thenReturn(true);
    when(postStatementService.countStatements(postId)).thenReturn(0L);
    when(userService.getById(userId)).thenReturn(admin);
    when(postStatementService.create(post, admin, 1, "입장문")).thenReturn(statement);

    postFacade.createStatement(postId, userId, PostStatus.COMPLETED, "입장문", imageIds);

    assertEquals(PostStatus.COMPLETED, post.getStatus());
    verify(postStatementService).create(post, admin, 1, "입장문");
    verify(imageService).attachImagesToStatement(admin, statement, imageIds);
  }

  @Test
  void createStatement_두번째입장문인데_최종상태를_보내면_FINAL_STATUS_NOT_ALLOWED() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.COMPLETED);

    when(postService.getByIdForUpdate(postId)).thenReturn(post);
    when(councilService.isAdmin(userId, council.getId())).thenReturn(true);
    when(postStatementService.countStatements(postId)).thenReturn(1L);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                postFacade.createStatement(postId, userId, PostStatus.REJECTED, "입장문", List.of()));

    assertSame(PostErrorCode.FINAL_STATUS_NOT_ALLOWED, exception.getErrorCode());
  }

  @Test
  void processClosing_APPROVED로_전이되면_알림을_생성한다() {
    UUID postId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);

    when(postService.get(postId)).thenReturn(post);
    when(voteService.countAgree(postId)).thenReturn(12L);
    when(voteService.countDisagree(postId)).thenReturn(8L);
    doAnswer(
            invocation -> {
              post.updateStatus(PostStatus.APPROVED);
              return null;
            })
        .when(postService)
        .close(postId, 12L, 20L);

    postFacade.processClosing(postId);

    verify(postService).close(postId, 12L, 20L);
    verify(notificationService).createPostApprovedNotifications(post);
  }

  @Test
  void processClosing_승인전이가_아니면_알림을_생성하지_않는다() {
    UUID postId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.8000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);

    when(postService.get(postId)).thenReturn(post);
    when(voteService.countAgree(postId)).thenReturn(7L);
    when(voteService.countDisagree(postId)).thenReturn(3L);
    doAnswer(
            invocation -> {
              post.updateStatus(PostStatus.PENDING);
              return null;
            })
        .when(postService)
        .close(postId, 7L, 10L);

    postFacade.processClosing(postId);

    verify(postService).close(postId, 7L, 10L);
    verify(notificationService, never()).createPostApprovedNotifications(any());
  }

  @Test
  void get_로그인사용자가_의회접근권한과_투표가능상태를_만족하면_canVote_true다() {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    User viewer = buildUser(userId, UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);
    when(postService.get(postId)).thenReturn(post);
    when(userService.getById(userId)).thenReturn(viewer);
    when(councilService.hasAccessToCouncil(viewer, council)).thenReturn(true);
    when(voteService.getSummary(postId)).thenReturn(new VoteSummaryResponse(12, 8, 20));
    when(postService.canCloseEarly(post, 12L, 20L)).thenReturn(true);
    when(imageService.getPostImages(postId))
        .thenReturn(List.of(PostImageView.of(UUID.randomUUID(), "https://example.com/image.png")));
    when(postStatementService.getStatements(postId)).thenReturn(List.of());
    when(imageService.getStatementImagesByStatementIds(List.of())).thenReturn(Map.of());

    PostDetailResponse result = postFacade.get(postId, userId);

    assertTrue(result.canVote());
    assertTrue(result.canCloseEarly());
    assertEquals(1, result.images().size());
    verify(postViewCountService).recordView(postId);
  }

  @Test
  void get_비로그인사용자면_canVote_false다() {
    UUID postId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), UserRole.STUDENT, "컴퓨터공학과");
    Council council = buildCouncil(UUID.randomUUID(), 10, new BigDecimal("0.5000"), author, "총학생회");
    Category category = buildCategory(UUID.randomUUID(), "학사");
    Post post = buildPost(postId, author, council, category, PostStatus.VOTING);
    when(postService.get(postId)).thenReturn(post);
    when(voteService.getSummary(postId)).thenReturn(new VoteSummaryResponse(2, 8, 10));
    when(postService.canCloseEarly(post, 2L, 10L)).thenReturn(false);
    when(imageService.getPostImages(postId)).thenReturn(List.of());
    when(postStatementService.getStatements(postId)).thenReturn(List.of());
    when(imageService.getStatementImagesByStatementIds(List.of())).thenReturn(Map.of());

    PostDetailResponse result = postFacade.get(postId, null);

    assertFalse(result.canVote());
    assertFalse(result.canCloseEarly());
  }

  private User buildUser(UUID id, UserRole role, String affiliationName) {
    Affiliation affiliation = Affiliation.builder().name(affiliationName).build();

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
    ReflectionTestUtils.setField(user, "role", role);
    return user;
  }

  private Council buildCouncil(
      UUID id, int minVote, BigDecimal approveRatioThreshold, User adminUser, String name) {
    Affiliation affiliation = Affiliation.builder().name(name).build();
    ReflectionTestUtils.setField(affiliation, "id", id);

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

  private Category buildCategory(UUID id, String name) {
    Category category = Category.builder().name(name).displayOrder(1).build();
    ReflectionTestUtils.setField(category, "id", id);
    return category;
  }

  private Post buildPost(
      UUID id, User author, Council council, Category category, PostStatus status) {
    Post post =
        Post.builder()
            .user(author)
            .council(council)
            .category(category)
            .title("제목")
            .content("본문")
            .postVotingDuration(PostVotingDuration.ONE_WEEK)
            .minVoteSnapshot(council.getMinVote())
            .approveRatioSnapshot(council.getApproveRatioThreshold())
            .createdAt(Instant.now(fixedClock))
            .build();
    ReflectionTestUtils.setField(post, "id", id);
    if (status != PostStatus.VOTING) {
      post.updateStatus(status);
    }
    return post;
  }
}
