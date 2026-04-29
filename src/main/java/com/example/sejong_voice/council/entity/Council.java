package com.example.sejong_voice.council.entity;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "council")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Council {

  @Id
  @Column(name = "affiliation_id", columnDefinition = "BINARY(16)")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "affiliation_id", nullable = false)
  private Affiliation affiliation;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User adminUser;

  @Column(name = "min_vote", nullable = false)
  private Integer minVote;

  @Column(name = "approve_ratio_threshold", nullable = false, precision = 5, scale = 4)
  private BigDecimal approveRatioThreshold;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder
  public Council(
      Affiliation affiliation,
      User adminUser,
      Integer minVote,
      BigDecimal approveRatioThreshold,
      Instant updatedAt
  ) {
    this.affiliation = affiliation;
    this.adminUser = adminUser;
    this.minVote = minVote;
    this.approveRatioThreshold = approveRatioThreshold;
    this.updatedAt = updatedAt;
  }

  public String getName() {
    return affiliation.getName();
  }

  public boolean isAdmin(UUID userId) {
    return adminUser.getId().equals(userId);
  }

  public void updatePolicy(Integer minVote, BigDecimal approveRatioThreshold, Instant updatedAt) {
    this.minVote = minVote;
    this.approveRatioThreshold = approveRatioThreshold;
    this.updatedAt = updatedAt;
  }
}
