package com.example.sejong_voice.report.entity;

import com.example.sejong_voice.comment.entity.Comment;
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
    name = "comment_report",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_comment_report_comment_user",
            columnNames = {"comment_id", "user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_comment_report_comment_id", columnList = "comment_id"),
        @Index(name = "idx_comment_report_user_id", columnList = "user_id"),
        @Index(name = "idx_comment_report_created_at", columnList = "created_at")
    }
)
public class CommentReport {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CommentReportReason reason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Builder
  public CommentReport(Comment comment, User user, CommentReportReason reason, Instant createdAt) {
    this.comment = comment;
    this.user = user;
    this.reason = reason;
    this.createdAt = createdAt;
  }
}
