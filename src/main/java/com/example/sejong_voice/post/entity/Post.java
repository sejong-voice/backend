package com.example.sejong_voice.post.entity;

import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "council_id", nullable = false)
  private Council council;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  private PostStatus status;

  @Column(nullable = false)
  private Instant votingStartAt;

  @Column(nullable = false)
  private Instant votingEndAt;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PostVotingDuration votingDuration;

  private Instant closedAt;

  @Column(nullable = false)
  private Integer minVoteSnapshot;

  @Column(nullable = false)
  private BigDecimal approveRatioSnapshot;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant deletedAt;

  @Column(nullable = false)
  private Integer viewCount;

  @Column(nullable = false)
  private Long agreeCount;

  @Column(nullable = false)
  private Long disagreeCount;

  @Column(nullable = false)
  private Long commentCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PostModerationStatus visibilityStatus;

  @Builder
  public Post(
      User user,
      Council council,
      Category category,
      String title,
      String content,
      PostVotingDuration postVotingDuration,
      Integer minVoteSnapshot,
      BigDecimal approveRatioSnapshot,
      Instant createdAt) {
    this.user = user;
    this.council = council;
    this.category = category;
    this.title = title;
    this.content = content;
    this.votingDuration = postVotingDuration;
    this.minVoteSnapshot = minVoteSnapshot;
    this.approveRatioSnapshot = approveRatioSnapshot;

    this.status = PostStatus.VOTING;

    this.createdAt = createdAt;
    this.votingStartAt = createdAt;
    this.votingEndAt = postVotingDuration.calculateDeadline(createdAt);

    this.viewCount = 1;
    this.agreeCount = 0L;
    this.disagreeCount = 0L;
    this.commentCount = 0L;
    this.visibilityStatus = PostModerationStatus.ACTIVE;
  }

  public void incrementViewCount() {
    this.viewCount++;
  }

  public void incrementViewCount(long amount) {
    this.viewCount += Math.toIntExact(amount);
  }

  public long getTotalVoteCount() {
    return agreeCount + disagreeCount;
  }

  public void incrementAgreeCount() {
    this.agreeCount++;
  }

  public void decrementAgreeCount() {
    this.agreeCount--;
  }

  public void incrementDisagreeCount() {
    this.disagreeCount++;
  }

  public void decrementDisagreeCount() {
    this.disagreeCount--;
  }

  public void incrementCommentCount() {
    this.commentCount++;
  }

  public void decrementCommentCount() {
    this.commentCount--;
  }

  public void delete(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  public void blind() {
    this.visibilityStatus = PostModerationStatus.BLINDED;
  }

  public void activate() {
    this.visibilityStatus = PostModerationStatus.ACTIVE;
  }

  public void close(Instant closedAt) {
    this.closedAt = closedAt;
  }

  public void updateStatus(PostStatus status) {
    this.status = status;
  }
}
