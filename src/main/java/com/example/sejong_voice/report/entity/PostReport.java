package com.example.sejong_voice.report.entity;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "post_report",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_post_report_post_reporter",
            columnNames = {"post_id", "reporter_user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_post_report_post_id", columnList = "post_id"),
        @Index(name = "idx_post_report_reporter_user_id", columnList = "reporter_user_id"),
        @Index(name = "idx_post_report_created_at", columnList = "created_at")
    }
)
public class PostReport {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_user_id", nullable = false)
  private User reporterUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PostReportReason reason;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Builder
  public PostReport(Post post, User reporterUser, PostReportReason reason, String description, Instant createdAt) {
    this.post = post;
    this.reporterUser = reporterUser;
    this.reason = reason;
    this.description = description;
    this.createdAt = createdAt;
  }
}
