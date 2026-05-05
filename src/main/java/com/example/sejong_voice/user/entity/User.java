package com.example.sejong_voice.user.entity;

import com.example.sejong_voice.affiliation.Affiliation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "service_agreed_at")
  private Instant serviceAgreedAt;

  @Column(name = "policy_agreed_at")
  private Instant policyAgreedAt;

  @Builder
  public User(
      String studentNo,
      String name,
      Affiliation affiliation, // affiliation 객체 자체를 넣어서 사용. user.affiliation.name이런식으로 사용
      String password,
      Instant duration,
      Instant createdAt) {
    this.studentNo = studentNo;
    this.name = name;
    this.affiliation = affiliation;
    this.password = password;

    // 생성 시점의 기본값 정의
    this.role = UserRole.STUDENT;
    this.status = UserStatus.ACTIVE;
    this.duration = duration != null ? duration : EPOCH;
    this.createdAt = createdAt;
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

  public void updateStatus(UserStatus status, Instant duration) { // 기간 정지 메서드
    this.status = status;
    this.duration = duration;
    this.deletedAt = null;
    // QUIT 후 관리자가 restore/정지/영구정지를 걸면 status는 바뀌는데 deleted_at은 남을 수 있다는 문제라서
    // ACTIVE, SUSPENDED, WITHDRAW로 변경할때는 deleted_at를 초기화해줌
  }

  public void withdrawNow() { // 영구정지 메서드
    this.status = UserStatus.WITHDRAWN;
    this.duration = PERMANENT_BAN_UNTIL;
    this.deletedAt = null;
    // QUIT 후 관리자가 restore/정지/영구정지를 걸면 status는 바뀌는데 deleted_at은 남을 수 있다는 문제라서
    // ACTIVE, SUSPENDED, WITHDRAW로 변경할때는 deleted_at를 초기화해줌
  }

  // 조기 복구
  public void activateNow() {
    this.status = UserStatus.ACTIVE;
    this.duration = EPOCH;
    this.deletedAt = null;
    // QUIT 후 관리자가 restore/정지/영구정지를 걸면 status는 바뀌는데 deleted_at은 남을 수 있다는 문제라서
    // ACTIVE, SUSPENDED, WITHDRAW로 변경할때는 deleted_at를 초기화해줌
  }

  public void updateRole(UserRole role) {
    this.role = role;
  }

  public String getDepartment() {
    return affiliation.getName();
  }

  // 약관/개인정보 동의 여부 확인
  public boolean hasRequiredConsents() {
    return serviceAgreedAt != null && policyAgreedAt != null;
  }

  // 약관/개인정보 모두 동의 처리
  public void agreeRequiredConsents(Instant serviceAgreedAt, Instant policyAgreedAt) {
    this.serviceAgreedAt = serviceAgreedAt;
    this.policyAgreedAt = policyAgreedAt;
  }

  // 탈퇴 처리(표시) 메서드
  public void markQuit(Instant now) {
    this.status = UserStatus.QUIT;
    this.deletedAt = now;
  }

  // 이미 탈퇴 처리된 상태
  public boolean isQuitPending() {
    if (this.status == UserStatus.QUIT && this.deletedAt == null) {
      // 데이터 정합성 깨진 케이스 감지용
      // Quit인데 deleated_at이 null
      // -> 로직에서는 해당 케이스가 발생하진 않지만 혹시나 정합성이 깨질 수 있으니, 확인 하기 위함
      log.warn("Inconsistent QUIT user detected. userId={}", this.studentNo);
    }
    return this.status == UserStatus.QUIT;
    // && this.deletedAt == null에 대한 예외 케이스는 log로 찍으니까 ㄱㅊ
  }
}
