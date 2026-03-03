package com.example.sejong_voice.council.entity;

import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "council_member",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_council_user", columnNames = {"council_id", "user_id"})
    }
)
public class CouncilMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "council_id", nullable = false)
  private Council council;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Builder
  public CouncilMember(Council council, User user) {
    this.council = council;
    this.user = user;
  }
}
