package com.example.sejong_voice.vote.entity;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(
    name = "vote",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vote_post_user", columnNames = {"post_id", "user_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private VoteChoice choice;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Builder
  public Vote(Post post, User user, VoteChoice choice) {
    this.post = post;
    this.user = user;
    this.choice = choice;
  }

  public void updateChoice(VoteChoice newChoice) {
    this.choice = newChoice;
  }
}
