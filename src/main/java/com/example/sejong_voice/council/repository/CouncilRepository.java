package com.example.sejong_voice.council.repository;

import com.example.sejong_voice.council.entity.Council;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouncilRepository extends JpaRepository<Council, UUID> {
  List<Council> findByNameContainingIgnoreCase(String keyword);
}
