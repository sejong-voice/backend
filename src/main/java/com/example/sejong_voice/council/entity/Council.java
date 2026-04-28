package com.example.sejong_voice.council.entity;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.policy.entity.CouncilPolicy;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "affiliation_id", nullable = false)
  private Affiliation affiliation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Council parent;

  @OneToOne(mappedBy = "council", cascade = CascadeType.ALL)
  private CouncilPolicy policy;

  @Builder
  public Council(Affiliation affiliation, Council parent) { //학생회 name대신에 affiliation 객체를 받아와야함
    this.affiliation = affiliation;
    this.parent = parent;
  }
}