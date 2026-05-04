package com.example.sejong_voice.comment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommentResponseTest {

  private final Instant now = Instant.parse("2026-05-01T00:00:00Z");

  @Test
  void from_일반학생댓글이면_익명표시명을_반환한다() {
    User author =
        TestFixtures.user(
            UUID.randomUUID(),
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
            UserRole.STUDENT,
            now);
    Post post = buildPost(author);
    Comment comment = TestFixtures.comment(UUID.randomUUID(), post, author, null, "댓글", now);

    CommentResponse response =
        CommentResponse.from(
            comment, UUID.randomUUID(), UUID.randomUUID(), Map.of(author.getId(), 1), List.of());

    assertEquals("익명1", response.authorName());
  }

  @Test
  void from_학생회계정댓글이면_실명을_반환한다() {
    User author =
        TestFixtures.user(
            UUID.randomUUID(),
            "20201234",
            "총학생회",
            TestFixtures.affiliation(UUID.randomUUID(), "총학생회"),
            UserRole.ADMIN,
            now);
    Post post = buildPost(author);
    Comment comment = TestFixtures.comment(UUID.randomUUID(), post, author, null, "댓글", now);

    CommentResponse response =
        CommentResponse.from(
            comment, UUID.randomUUID(), UUID.randomUUID(), Map.of(author.getId(), 1), List.of());

    assertEquals("총학생회", response.authorName());
  }

  @Test
  void from_게시글작성자댓글이면_익명글쓴이표시명을_반환한다() {
    User author =
        TestFixtures.user(
            UUID.randomUUID(),
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
            UserRole.STUDENT,
            now);
    Post post = buildPost(author);
    Comment comment = TestFixtures.comment(UUID.randomUUID(), post, author, null, "댓글", now);

    CommentResponse response =
        CommentResponse.from(comment, UUID.randomUUID(), author.getId(), Map.of(), List.of());

    assertEquals("익명(글쓴이)", response.authorName());
  }

  @Test
  void from_삭제된댓글이면_작성자명을_삭제로_반환한다() {
    User author =
        TestFixtures.user(
            UUID.randomUUID(),
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
            UserRole.STUDENT,
            now);
    Post post = buildPost(author);
    Comment comment = TestFixtures.comment(UUID.randomUUID(), post, author, null, "댓글", now);
    comment.delete(now);

    CommentResponse response =
        CommentResponse.from(
            comment, UUID.randomUUID(), UUID.randomUUID(), Map.of(author.getId(), 1), List.of());

    assertEquals("(삭제)", response.authorName());
    assertEquals(CommentStatus.DELETED, response.status());
    assertEquals(false, response.canDelete());
    assertEquals(false, response.postAuthor());
  }

  @Test
  void from_숨김댓글이면_작성자명을_숨김으로_반환한다() {
    User author =
        TestFixtures.user(
            UUID.randomUUID(),
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
            UserRole.STUDENT,
            now);
    Post post = buildPost(author);
    Comment comment = TestFixtures.comment(UUID.randomUUID(), post, author, null, "댓글", now);
    comment.blind();

    CommentResponse response =
        CommentResponse.from(
            comment, UUID.randomUUID(), UUID.randomUUID(), Map.of(author.getId(), 1), List.of());

    assertEquals("(숨김)", response.authorName());
    assertEquals(CommentStatus.BLINDED, response.status());
    assertEquals(false, response.canDelete());
    assertEquals(false, response.postAuthor());
  }

  private Post buildPost(User author) {
    return TestFixtures.post(
        UUID.randomUUID(),
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
