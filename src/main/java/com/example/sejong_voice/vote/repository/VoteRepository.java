package com.example.sejong_voice.vote.repository;

import com.example.sejong_voice.post.dto.PostCountRow;
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
      SELECT new com.example.sejong_voice.post.dto.PostCountRow(v.post.id, COUNT(v))
      FROM Vote v
      WHERE v.post.id IN :postIds
      GROUP BY v.post.id
      """)
  List<PostCountRow> countByPostIds(@Param("postIds") Collection<UUID> postIds);

  @Query("""
      SELECT v
      FROM Vote v
      JOIN FETCH v.user
      WHERE v.post.id = :postId
      ORDER BY COALESCE(v.updatedAt, v.createdAt) ASC, v.createdAt ASC
      """)
  List<Vote> findAllByPostIdWithUserOrderByVotedAtAsc(@Param("postId") UUID postId);
}
