package com.example.sejong_voice.post.vote;

import com.example.sejong_voice.post.vote.entity.Vote;
import com.example.sejong_voice.post.vote.entity.VoteChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostVoteRepository extends JpaRepository<Vote, UUID> {

  Optional<Vote> findByPostIdAndUserId(UUID postId, UUID userId);

  Long countByPostIdAndChoice(UUID postId, VoteChoice choice);
}