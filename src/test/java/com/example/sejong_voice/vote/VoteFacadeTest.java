package com.example.sejong_voice.vote;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.PostService;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.VoteChoice;
import com.example.sejong_voice.vote.exception.VoteErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VoteFacadeTest {

  @Mock private VoteService voteService;
  @Mock private UserService userService;
  @Mock private PostService postService;
  @Mock private CouncilService councilService;

  private VoteFacade voteFacade;
  private Clock fixedClock;
  private Instant now;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    voteFacade = new VoteFacade(voteService, userService, postService, councilService, fixedClock);
    now = Instant.now(fixedClock);
  }

  @Test
  void cast_접근권한이_없으면_ACCESS_DENIED() {
    User user = buildUser(UserRole.STUDENT, "컴퓨터공학과");
    Post post = buildPost(PostStatus.VOTING, now.plusSeconds(60), user);
    when(userService.getById(user.getId())).thenReturn(user);
    when(postService.get(post.getId())).thenReturn(post);
    when(councilService.hasAccessToCouncil(user, post.getCouncil())).thenReturn(false);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> voteFacade.cast(post.getId(), user.getId(), VoteChoice.AGREE));

    assertSame(PostErrorCode.ACCESS_DENIED, exception.getErrorCode());
  }

  @Test
  void cast_투표마감이면_VOTE_CLOSED() {
    User user = buildUser(UserRole.STUDENT, "컴퓨터공학과");
    Post post = buildPost(PostStatus.VOTING, now.minusSeconds(1), user);
    when(userService.getById(user.getId())).thenReturn(user);
    when(postService.get(post.getId())).thenReturn(post);
    when(councilService.hasAccessToCouncil(user, post.getCouncil())).thenReturn(true);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> voteFacade.cast(post.getId(), user.getId(), VoteChoice.AGREE));

    assertSame(VoteErrorCode.VOTE_CLOSED, exception.getErrorCode());
    verify(voteService, never()).cast(post, user, VoteChoice.AGREE);
  }

  @Test
  void cast_정상투표면_서비스에_위임한다() {
    User user = buildUser(UserRole.STUDENT, "컴퓨터공학과");
    Post post = buildPost(PostStatus.VOTING, now.plusSeconds(60), user);
    when(userService.getById(user.getId())).thenReturn(user);
    when(postService.get(post.getId())).thenReturn(post);
    when(councilService.hasAccessToCouncil(user, post.getCouncil())).thenReturn(true);

    voteFacade.cast(post.getId(), user.getId(), VoteChoice.AGREE);

    verify(voteService).cast(post, user, VoteChoice.AGREE);
  }

  @Test
  void getSummary_서비스결과를_반환한다() {
    UUID postId = UUID.randomUUID();
    VoteSummaryResponse response = new VoteSummaryResponse(1, 2, 3);
    when(voteService.getSummary(postId)).thenReturn(response);

    assertSame(response, voteFacade.getSummary(postId));
  }

  @Test
  void getStatistics_권한이_없으면_ACCESS_DENIED() {
    User user = buildUser(UserRole.STUDENT, "컴퓨터공학과");
    Post post = buildPost(PostStatus.APPROVED, now.plusSeconds(60), user);
    when(postService.get(post.getId())).thenReturn(post);
    when(userService.getById(user.getId())).thenReturn(user);
    when(councilService.isAdmin(user.getId(), post.getCouncil().getId())).thenReturn(false);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> voteFacade.getStatistics(post.getId(), user.getId()));

    assertSame(PostErrorCode.ACCESS_DENIED, exception.getErrorCode());
  }

  @Test
  void getStatistics_게시글상태가_미완료면_VOTE_DETAILS_NOT_AVAILABLE() {
    User user = buildUser(UserRole.SUPER, "컴퓨터공학과");
    Post post = buildPost(PostStatus.VOTING, now.plusSeconds(60), user);
    when(postService.get(post.getId())).thenReturn(post);
    when(userService.getById(user.getId())).thenReturn(user);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> voteFacade.getStatistics(post.getId(), user.getId()));

    assertSame(PostErrorCode.VOTE_DETAILS_NOT_AVAILABLE, exception.getErrorCode());
  }

  @Test
  void getAgreeVoters_슈퍼유저면_서비스에_위임한다() {
    User user = buildUser(UserRole.SUPER, "컴퓨터공학과");
    Post post = buildPost(PostStatus.APPROVED, now.plusSeconds(60), user);
    when(postService.get(post.getId())).thenReturn(post);
    when(userService.getById(user.getId())).thenReturn(user);
    when(voteService.getAgreeVoters(post)).thenReturn(List.of());

    voteFacade.getAgreeVoters(post.getId(), user.getId());

    verify(voteService).getAgreeVoters(post);
  }

  private User buildUser(UserRole role, String department) {
    return TestFixtures.user(
        UUID.randomUUID(),
        "20201234",
        "홍길동",
        TestFixtures.affiliation(UUID.randomUUID(), department),
        role,
        now);
  }

  private Post buildPost(PostStatus status, Instant votingEndAt, User adminUser) {
    Affiliation affiliation = TestFixtures.affiliation(UUID.randomUUID(), "총학생회");
    Council council =
        TestFixtures.council(
            affiliation.getId(), affiliation, adminUser, 10, new BigDecimal("0.5000"), now);
    Category category = TestFixtures.category(UUID.randomUUID(), "학사");
    Post post =
        TestFixtures.post(
            UUID.randomUUID(),
            adminUser,
            council,
            category,
            status,
            10,
            new BigDecimal("0.5000"),
            now);
    ReflectionTestUtils.setField(post, "votingEndAt", votingEndAt);
    return post;
  }
}
