package com.example.sejong_voice.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.post.dto.PostStatementView;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.repository.PostStatementRepository;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
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
class PostStatementServiceTest {

  @Mock private PostStatementRepository postStatementRepository;

  private PostStatementService postStatementService;
  private Clock fixedClock;
  private Instant now;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    postStatementService = new PostStatementService(postStatementRepository, fixedClock);
    now = Instant.now(fixedClock);
  }

  @Test
  void create_입장문을_생성한다() {
    User author = buildUser(UUID.randomUUID());
    Post post = buildPost(UUID.randomUUID(), author);
    when(postStatementRepository.save(org.mockito.ArgumentMatchers.any(PostStatement.class)))
        .thenAnswer(
            invocation -> {
              PostStatement statement = invocation.getArgument(0);
              ReflectionTestUtils.setField(
                  statement, "id", UUID.fromString("00000000-0000-0000-0000-000000000123"));
              return statement;
            });

    PostStatement result = postStatementService.create(post, author, 1, "입장문");

    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000123"), result.getId());
    assertEquals(1, result.getSequence());
    assertEquals(now, result.getCreatedAt());
  }

  @Test
  void countStatements_리포지토리결과를_반환한다() {
    UUID postId = UUID.randomUUID();
    when(postStatementRepository.countByPostId(postId)).thenReturn(2L);

    assertEquals(2L, postStatementService.countStatements(postId));
  }

  @Test
  void getStatements_엔티티를_view로_변환한다() {
    User author = buildUser(UUID.randomUUID());
    Post post = buildPost(UUID.randomUUID(), author);
    PostStatement statement =
        TestFixtures.postStatement(UUID.randomUUID(), post, author, 1, "입장문", now);
    when(postStatementRepository.findAllByPostIdOrderBySequenceAsc(post.getId()))
        .thenReturn(List.of(statement));

    List<PostStatementView> result = postStatementService.getStatements(post.getId());

    assertEquals(1, result.size());
    assertEquals(statement.getId(), result.get(0).id());
    verify(postStatementRepository).findAllByPostIdOrderBySequenceAsc(post.getId());
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
