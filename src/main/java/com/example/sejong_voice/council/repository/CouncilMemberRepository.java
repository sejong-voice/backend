package com.example.sejong_voice.council.repository;

import com.example.sejong_voice.council.entity.CouncilMember;
import com.example.sejong_voice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, UUID> {

    /**
     * 유저 ID와 학생회 ID로 해당 유저가 해당 학생회의 멤버(관리 권한자)인지 확인합니다.
     */
    boolean existsByUserIdAndCouncilId(UUID userId, UUID councilId);


    @Query("select cm.user from CouncilMember cm where cm.council.id = :councilId")
    Optional<User> findUserByCouncilId(@Param("councilId") UUID councilId);
}
