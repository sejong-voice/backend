package com.example.sejong_voice.support;

import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.entity.PostImage;
import com.example.sejong_voice.image.entity.PostStatementImage;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.vote.entity.Vote;
import com.example.sejong_voice.vote.entity.VoteChoice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestFixtures {

  private TestFixtures() {}

  public static UserPrincipal principal(UUID userId, UserRole role) {
    return UserPrincipal.builder().userId(userId).studentNo("20201234").role(role).build();
  }

  public static Affiliation affiliation(UUID id, String name) {
    Affiliation affiliation = Affiliation.builder().name(name).build();
    ReflectionTestUtils.setField(affiliation, "id", id);
    return affiliation;
  }

  public static Affiliation affiliation(UUID id, String name, Affiliation parent) {
    Affiliation affiliation = Affiliation.builder().name(name).parent(parent).build();
    ReflectionTestUtils.setField(affiliation, "id", id);
    return affiliation;
  }

  public static User user(
      UUID id,
      String studentNo,
      String name,
      Affiliation affiliation,
      UserRole role,
      Instant createdAt) {
    User user =
        User.builder()
            .studentNo(studentNo)
            .name(name)
            .affiliation(affiliation)
            .password("ENC_PW")
            .duration(User.activeDuration())
            .createdAt(createdAt)
            .build();
    ReflectionTestUtils.setField(user, "id", id);
    ReflectionTestUtils.setField(user, "role", role);
    return user;
  }

  public static Category category(UUID id, String name) {
    Category category = Category.builder().name(name).displayOrder(1).build();
    ReflectionTestUtils.setField(category, "id", id);
    return category;
  }

  public static Council council(
      UUID id,
      Affiliation affiliation,
      User adminUser,
      int minVote,
      BigDecimal approveRatioThreshold,
      Instant updatedAt) {
    Council council =
        Council.builder()
            .affiliation(affiliation)
            .adminUser(adminUser)
            .minVote(minVote)
            .approveRatioThreshold(approveRatioThreshold)
            .updatedAt(updatedAt)
            .build();
    ReflectionTestUtils.setField(council, "id", id);
    return council;
  }

  public static Post post(
      UUID id,
      User user,
      Council council,
      Category category,
      PostStatus status,
      int minVoteSnapshot,
      BigDecimal approveRatioSnapshot,
      Instant createdAt) {
    Post post =
        Post.builder()
            .user(user)
            .council(council)
            .category(category)
            .title("title")
            .content("content")
            .postVotingDuration(PostVotingDuration.ONE_WEEK)
            .minVoteSnapshot(minVoteSnapshot)
            .approveRatioSnapshot(approveRatioSnapshot)
            .createdAt(createdAt)
            .build();
    ReflectionTestUtils.setField(post, "id", id);
    if (status != PostStatus.VOTING) {
      post.updateStatus(status);
    }
    return post;
  }

  public static Comment comment(
      UUID id, Post post, User user, Comment rootComment, String content, Instant createdAt) {
    Comment comment =
        Comment.builder()
            .post(post)
            .user(user)
            .rootComment(rootComment)
            .content(content)
            .createdAt(createdAt)
            .build();
    ReflectionTestUtils.setField(comment, "id", id);
    return comment;
  }

  public static PostStatement postStatement(
      UUID id, Post post, User createdBy, int sequence, String content, Instant createdAt) {
    PostStatement statement =
        PostStatement.builder()
            .post(post)
            .createdBy(createdBy)
            .sequence(sequence)
            .content(content)
            .createdAt(createdAt)
            .build();
    ReflectionTestUtils.setField(statement, "id", id);
    return statement;
  }

  public static Vote vote(
      UUID id, Post post, User user, VoteChoice choice, Instant createdAt, Instant updatedAt) {
    Vote vote = Vote.builder().post(post).user(user).choice(choice).build();
    ReflectionTestUtils.setField(vote, "id", id);
    ReflectionTestUtils.setField(vote, "createdAt", createdAt);
    ReflectionTestUtils.setField(vote, "updatedAt", updatedAt);
    return vote;
  }

  public static Image image(UUID id, User user, String objectName, Instant createdAt) {
    Image image =
        Image.builder()
            .user(user)
            .originalFileName("image.png")
            .objectName(objectName)
            .contentType("image/png")
            .size(100L)
            .createdAt(createdAt)
            .build();
    ReflectionTestUtils.setField(image, "id", id);
    return image;
  }

  public static PostImage postImage(
      UUID id, Post post, Image image, int displayOrder, Instant createdAt) {
    PostImage postImage =
        PostImage.builder()
            .post(post)
            .image(image)
            .displayOrder(displayOrder)
            .createdAt(createdAt)
            .build();
    ReflectionTestUtils.setField(postImage, "id", id);
    return postImage;
  }

  public static PostStatementImage postStatementImage(
      UUID id, PostStatement statement, Image image, int displayOrder, Instant createdAt) {
    PostStatementImage postStatementImage =
        PostStatementImage.builder()
            .postStatement(statement)
            .image(image)
            .displayOrder(displayOrder)
            .createdAt(createdAt)
            .build();
    ReflectionTestUtils.setField(postStatementImage, "id", id);
    return postStatementImage;
  }
}
