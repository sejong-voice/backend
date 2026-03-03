package com.example.sejong_voice.policy.entity;

import com.example.sejong_voice.council.entity.Council;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouncilPolicy {

  @Id
  @Column(name = "council_id")
  private UUID id;

  @OneToOne
  @MapsId
  @JoinColumn(name = "council_id")
  private Council council;

  @Column(nullable = false)
  private Integer minVote;

  @Column(nullable = false, precision = 5, scale = 4)
  private BigDecimal approveRatioThreshold;

  @Builder
  public CouncilPolicy(Council council, Integer minVote, BigDecimal approveRatioThreshold) {
    this.council = council;
    this.minVote = minVote;
    this.approveRatioThreshold = approveRatioThreshold;
  }

  public void update(Integer minVote, BigDecimal approveRatioThreshold) {
    this.minVote = minVote;
    this.approveRatioThreshold = approveRatioThreshold;
  }
}