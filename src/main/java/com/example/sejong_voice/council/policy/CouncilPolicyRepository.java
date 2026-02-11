package com.example.sejong_voice.council.policy;

import com.example.sejong_voice.council.policy.entity.CouncilPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouncilPolicyRepository extends JpaRepository<CouncilPolicy, UUID> {

}