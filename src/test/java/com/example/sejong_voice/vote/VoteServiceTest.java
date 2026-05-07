package com.example.sejong_voice.vote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.post.dto.PostCountRow;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.vote.dto.VoteAgreeVoterResponse;
import com.example.sejong_voice.vote.dto.VoteStatisticsResponse;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.Vote;
import com.example.sejong_voice.vote.entity.VoteChoice;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.vote.repository.VoteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

  @Mock private VoteRepository voteRepository;
  @Mock private PostRepository postRepository;

  private VoteService voteService;
  private Instant now;

  @BeforeEach
  void setUp() {
    voteService = new VoteService(voteRepository, postRepository);
    now = Instant.parse("2026-05-01T00:00:00Z");
  }

  @Test
  void cast_기존투표가_없으면_새로_생성한다() {
    Post post = buildPost();
    User user = buildUser("컴퓨터공학과");
    when(voteRepository.findByPostIdAndUserId(post.getId(), user.getId()))
        .thenReturn(Optional.empty());

    voteService.cast(post, user, VoteChoice.AGREE);

    ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
    verify(voteRepository).save(captor.capture());
    assertEquals(VoteChoice.AGREE, captor.getValue().getChoice());
  }

  @Test
  void cast_기존투표가_있으면_선택지만_변경한다() {
    Post post = buildPost();
    User user = buildUser("컴퓨터공학과");
    Vote vote = buildVote(post, user, VoteChoice.DISAGREE);
    when(voteRepository.findByPostIdAndUserId(post.getId(), user.getId()))
        .thenReturn(Optional.of(vote));

    voteService.cast(post, user, VoteChoice.AGREE);

    assertEquals(VoteChoice.AGREE, vote.getChoice());
    verify(voteRepository, never()).save(any());
  }

  @Test
  void cast_동시성으로_저장충돌이_나면_기존투표를_다시조회해_변경한다() {
    Post post = buildPost();
    User user = buildUser("컴퓨터공학과");
    Vote vote = buildVote(post, user, VoteChoice.DISAGREE);
    when(voteRepository.findByPostIdAndUserId(post.getId(), user.getId()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(vote));
    when(voteRepository.save(any(Vote.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate vote"));

    voteService.cast(post, user, VoteChoice.AGREE);

    assertEquals(VoteChoice.AGREE, vote.getChoice());
  }

  @Test
  void getSummary_찬반개수와_총합을_반환한다() {
    UUID postId = UUID.randomUUID();
    Post post = buildPost();
    org.springframework.test.util.ReflectionTestUtils.setField(post, "id", postId);
    org.springframework.test.util.ReflectionTestUtils.setField(post, "agreeCount", 7L);
    org.springframework.test.util.ReflectionTestUtils.setField(post, "disagreeCount", 3L);
    when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));

    VoteSummaryResponse result = voteService.getSummary(postId);

    assertEquals(7L, result.agreeCount());
    assertEquals(3L, result.disagreeCount());
    assertEquals(10L, result.totalCount());
  }

  @Test
  void getStatistics_학과별집계와_비율을_반환한다() {
    Post post = buildPost();
    User csUser = buildUser("컴퓨터공학과");
    User eeUser = buildUser("전자정보공학과");
    Vote vote1 = buildVote(post, csUser, VoteChoice.AGREE);
    Vote vote2 = buildVote(post, csUser, VoteChoice.DISAGREE);
    Vote vote3 = buildVote(post, eeUser, VoteChoice.AGREE);
    when(voteRepository.findAllByPostIdWithUserOrderByVotedAtAsc(post.getId()))
        .thenReturn(List.of(vote1, vote2, vote3));

    VoteStatisticsResponse result = voteService.getStatistics(post);

    assertEquals(2L, result.agreeCount());
    assertEquals(1L, result.disagreeCount());
    assertEquals(3L, result.totalCount());
    assertEquals(new BigDecimal("0.6667"), result.agreeRatio());
    assertEquals("컴퓨터공학과", result.departmentDistributions().get(0).department());
    assertEquals(2L, result.departmentDistributions().get(0).totalCount());
  }

  @Test
  void getAgreeVoters_찬성표만_응답으로_변환한다() {
    Post post = buildPost();
    User user = buildUser("컴퓨터공학과");
    Vote vote = buildVote(post, user, VoteChoice.AGREE);
    when(voteRepository.findAllByPostIdAndChoiceWithUserOrderByVotedAtTimeAsc(
            post.getId(), VoteChoice.AGREE))
        .thenReturn(List.of(vote));

    List<VoteAgreeVoterResponse> result = voteService.getAgreeVoters(post);

    assertEquals(1, result.size());
    assertEquals(user.getStudentNo(), result.get(0).studentNo());
  }

  @Test
  void countByPostIds_빈목록이면_빈맵이다() {
    assertTrue(voteService.countByPostIds(List.of()).isEmpty());
    verify(voteRepository, never()).countByPostIds(any());
  }

  @Test
  void countByPostIds_집계결과를_맵으로_반환한다() {
    UUID postId1 = UUID.randomUUID();
    UUID postId2 = UUID.randomUUID();
    when(voteRepository.countByPostIds(List.of(postId1, postId2)))
        .thenReturn(List.of(new PostCountRow(postId1, 4L), new PostCountRow(postId2, 8L)));

    Map<UUID, Long> result = voteService.countByPostIds(List.of(postId1, postId2));

    assertEquals(4L, result.get(postId1));
    assertEquals(8L, result.get(postId2));
  }

  private Post buildPost() {
    Affiliation affiliation = TestFixtures.affiliation(UUID.randomUUID(), "총학생회");
    User author =
        TestFixtures.user(UUID.randomUUID(), "20201234", "홍길동", affiliation, UserRole.STUDENT, now);
    Council council =
        TestFixtures.council(
            affiliation.getId(), affiliation, author, 10, new BigDecimal("0.5000"), now);
    Category category = TestFixtures.category(UUID.randomUUID(), "학사");
    return TestFixtures.post(
        UUID.randomUUID(),
        author,
        council,
        category,
        PostStatus.VOTING,
        10,
        new BigDecimal("0.5000"),
        now);
  }

  private User buildUser(String department) {
    return TestFixtures.user(
        UUID.randomUUID(),
        "20201234",
        "홍길동",
        TestFixtures.affiliation(UUID.randomUUID(), department),
        UserRole.STUDENT,
        now);
  }

  private Vote buildVote(Post post, User user, VoteChoice choice) {
    return TestFixtures.vote(UUID.randomUUID(), post, user, choice, now, null);
  }
}
