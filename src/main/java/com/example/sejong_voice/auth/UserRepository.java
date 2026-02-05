package com.example.sejong_voice.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    //1. 학번으로 사용자 조회
    Optional<User> findByStudentNo(String studentNo);
    //2. 특정 학과 모든 사용자 조회
    List<User> findByDepartment(String department);
    //3. 권한 상태 확인
    List<User> findByRole(UserRole role);
    //4. 계정 상태 확인
    Optional<User> findByStatus(UserStatus status);
    //5. 학번 존재 여부 확인(중복 체크)
    boolean existsByStudentNo(String studentNo);
}
