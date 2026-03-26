package com.example.sejong_voice.vote.repository;

import com.example.sejong_voice.vote.entity.Vote;
import com.example.sejong_voice.vote.entity.VoteChoice;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

  Optional<Vote> findByPostIdAndUserId(UUID postId, UUID userId);

  Long countByPostIdAndChoice(UUID postId, VoteChoice choice);

  boolean existsByPostId(UUID postId);

  @Query("""
      SELECT v.post.id, COUNT(v)
      FROM Vote v
      WHERE v.post.id IN :postIds
      GROUP BY v.post.id
      """)
  List<Object[]> countByPostIds(@Param("postIds") Collection<UUID> postIds);
}
