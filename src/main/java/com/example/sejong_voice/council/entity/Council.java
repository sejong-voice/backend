package com.example.sejong_voice.council.entity;

import com.example.sejong_voice.council.policy.entity.CouncilPolicy;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Council {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Council parent;

  @OneToOne(mappedBy = "council", cascade = CascadeType.ALL)
  private CouncilPolicy policy;

  @Builder
  public Council(String name, Council parent) {
    this.name = name;
    this.parent = parent;
  }
}