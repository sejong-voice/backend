package com.example.sejong_voice.user;

import com.example.sejong_voice.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  // 1. 학번으로 사용자 조회
  Optional<User> findByStudentNo(String studentNo);
}
