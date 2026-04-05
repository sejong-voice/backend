package com.example.sejong_voice.user.entity;

import com.example.sejong_voice.auth.dto.DepartmentRequest;
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

  @Column(name = "department", nullable = false)
  private String department;

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

  @Builder
  public User(
      String studentNo,
      String name,
      String department,
      String password,
      Instant duration,
      Instant createdAt
  ) {
    this.studentNo = studentNo;
    this.name = name;
    this.department = department;
    this.password = password;

    // 생성 시점의 기본값 정의
    this.role = UserRole.STUDENT;
    this.status = UserStatus.ACTIVE;
    this.duration = duration != null ? duration : EPOCH;
    this.createdAt = createdAt;
  }


  public void updateDepartment(DepartmentRequest newDepartment) {
    this.department = newDepartment.department();
  }

  public void updatePassword(String newPassword){this.password = newPassword;}

  public void updateStatus(UserStatus status, Instant duration) {
    this.status = status;
    this.duration = duration;
  }

  // 조기 복구
  public void activateNow(Instant duration){
    this.status = UserStatus.ACTIVE;
    this.duration = duration;
  }

  public void updateRole(UserRole role) {
    this.role = role;
  }
}
