package com.example.sejong_voice.policy.repository;

import com.example.sejong_voice.policy.entity.CouncilPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<CouncilPolicy, UUID> {

}