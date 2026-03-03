package com.example.sejong_voice.council.repository;

import com.example.sejong_voice.council.entity.CouncilMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, UUID> {
    
  /**
   * 유저 ID와 학생회 ID로 해당 유저가 해당 학생회의 멤버(관리 권한자)인지 확인합니다.
   */
  boolean existsByUserIdAndCouncilId(UUID userId, UUID councilId);
}
