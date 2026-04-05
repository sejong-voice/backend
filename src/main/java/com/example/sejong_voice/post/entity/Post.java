package com.example.sejong_voice.post.entity;

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
import java.math.RoundingMode;
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

  @Column(columnDefinition = "TEXT")
  private String resultContent;

  private Instant resultCreatedAt;

  private Instant resultUpdatedAt;

  @Column(nullable = false)
  private Integer viewCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PostModerationStatus visibilityStatus;

  @Builder
  public Post(
      User user,
      Council council,
      String title,
      String content,
      PostVotingDuration postVotingDuration,
      Integer minVoteSnapshot,
      BigDecimal approveRatioSnapshot,
      Instant createdAt
  ) {
    this.user = user;
    this.council = council;
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
    this.visibilityStatus = PostModerationStatus.ACTIVE;
  }

  public void incrementViewCount() {
    this.viewCount++;
  }

  public void update(String newTitle, String newContent) {
    this.title = newTitle;
    this.content = newContent;
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

  public void evaluateAndClose(long agreeCount, long totalCount, Instant closedAt) {
    if (this.status != PostStatus.VOTING) {
      return;
    }

    this.closedAt = closedAt;

    // 1. 최소 투표 수 미달 -> 기각
    if (totalCount < this.minVoteSnapshot) {
      this.status = PostStatus.PENDING;
      return;
    }

    // 2. 찬성 비율 미달 -> 기각
    BigDecimal actualRatio = totalCount == 0 ? BigDecimal.ZERO :
        BigDecimal.valueOf(agreeCount)
            .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP);

    if (actualRatio.compareTo(this.approveRatioSnapshot) < 0) {
      this.status = PostStatus.PENDING;
      return;
    }

    // 3. 통과 -> 답변 대기 상태
    this.status = PostStatus.APPROVED;
  }

  public void updateResult(PostStatus status, String resultContent, Instant now) {
    this.status = status;
    this.resultContent = resultContent;

    if (this.resultCreatedAt == null) {
      this.resultCreatedAt = now;
      return;
    }

    this.resultUpdatedAt = now;
  }
}
