package com.example.sejong_voice.vote.repository;

import com.example.sejong_voice.vote.entity.Vote;
import com.example.sejong_voice.vote.entity.VoteChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

  Optional<Vote> findByPostIdAndUserId(UUID postId, UUID userId);

  Long countByPostIdAndChoice(UUID postId, VoteChoice choice);

  boolean existsByPostId(UUID postId);
}