package com.example.sejong_voice.user;

import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
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
    //3. 권한 상태 확인
    //4. 계정 상태 확인
    //5. 학번 존재 여부 확인(중복 체크)
}
