package com.example.sejong_voice.user.entity;

import com.example.sejong_voice.affiliation.Affiliation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

  private static final Instant EPOCH = Instant.EPOCH;
  private static final Instant PERMANENT_BAN_UNTIL = Instant.parse("9999-12-31T23:59:59Z");


  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(name = "student_no", length = 30, nullable = false, unique = true)
  private String studentNo;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "name", length = 60, nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "affiliation_id", nullable = false)
  private Affiliation affiliation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  @Column(nullable = false)
  private Instant duration;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "service_agreed", nullable = false)
  private boolean serviceAgreed;

  @Column(name = "policy_agreed", nullable = false)
  private boolean policyAgreed;



  @Builder
  public User(
      String studentNo,
      String name,
      Affiliation affiliation, // affiliation 객체 자체를 넣어서 사용. user.affiliation.name이런식으로 사용
      String password,
      Instant duration,
      Instant createdAt
  ) {
    this.studentNo = studentNo;
    this.name = name;
    this.affiliation = affiliation;
    this.password = password;

    // 생성 시점의 기본값 정의
    this.role = UserRole.STUDENT;
    this.status = UserStatus.ACTIVE;
    this.duration = duration != null ? duration : EPOCH;
    this.createdAt = createdAt;
    // 생성 기본값
    this.serviceAgreed = false;
    this.policyAgreed = false;

  }


  public static Instant activeDuration() {
    return EPOCH;
  }

  public static Instant withdrawnDuration() {
    return PERMANENT_BAN_UNTIL;
  }

  public void updatePassword(String newPassword) {
    this.password = newPassword;
  }

  public void updateStatus(UserStatus status, Instant duration) {
    this.status = status;
    this.duration = duration;
  }

  public void withdrawNow() {// 영구정지 메서드
    this.status = UserStatus.WITHDRAWN;
    this.duration = PERMANENT_BAN_UNTIL;
  }

  // 조기 복구
  public void activateNow() {
    this.status = UserStatus.ACTIVE;
    this.duration = EPOCH;
  }

  public void updateRole(UserRole role) {
    this.role = role;
  }

  public String getDepartment() {
    return affiliation.getName();
  }

  // 약관/개인정보 동의 여부 확인
  public boolean hasRequiredConsents() {
    return serviceAgreed && policyAgreed;
  }

  // 약관/개인정보 모두 동의 처리
  public void agreeRequiredConsents() {
    this.serviceAgreed = true;
    this.policyAgreed = true;
  }
}
