package com.example.sejong_voice.user.entity;

import com.example.sejong_voice.auth.dto.DepartmentRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

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
  private LocalDateTime duration;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  
  // 특정 필드만 인자로 받는 생성자에 @Builder 적용
  @Builder
  public User(String studentNo, String name, String department, String password) {
    this.studentNo = studentNo;
    this.name = name;
    this.department = department;
    this.password = password;

    // 생성 시점의 기본값 정의
    this.role = UserRole.STUDENT;
    this.status = UserStatus.ACTIVE;
    this.duration = LocalDateTime.now(); // '0'의 의미로 현재 시간 세팅
    this.createdAt = LocalDateTime.now();
  }

  // 비즈니스 로직에 따른 업데이트 메서드 -> role / status-duration따로따로 dto 마련해서 업데이트 하는 구조
  public void updateDepartment(DepartmentRequest newDepartment) {
    this.department = newDepartment.department();
  }

  public void updateStatus(UserStatus status, LocalDateTime duration) {
    this.status = status;
    this.duration = duration;
  }

  public void updateRole(UserRole role) {
    this.role = role;
  }
}