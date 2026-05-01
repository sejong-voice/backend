package com.example.sejong_voice.history.entity;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostModerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "post_status_history",
    indexes = {
      @Index(name = "idx_post_status_history_post_id", columnList = "post_id"),
      @Index(name = "idx_post_status_history_user_id", columnList = "user_id"),
      @Index(name = "idx_post_status_history_created_at", columnList = "created_at")
    })
public class PostStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)") //
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 40)
  private PostModerationStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 40)
  private PostModerationStatus toStatus;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Builder
  public PostStatusHistory(
      UUID userId, // 상태를 변경한 관리자 id
      Post post, // 상태가 변경된 post
      PostModerationStatus fromStatus, // before 상태
      PostModerationStatus toStatus, // after 상태
      Instant createdAt) {
    this.userId = userId;
    this.post = post;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.createdAt = createdAt;
  }
}
