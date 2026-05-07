package com.example.sejong_voice.vote.repository;

import com.example.sejong_voice.post.dto.PostCountRow;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
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

  @Query(
      """
      SELECT new com.example.sejong_voice.vote.dto.VoteSummaryResponse(
        COALESCE(SUM(CASE WHEN v.choice = com.example.sejong_voice.vote.entity.VoteChoice.AGREE THEN 1 ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN v.choice = com.example.sejong_voice.vote.entity.VoteChoice.DISAGREE THEN 1 ELSE 0 END), 0),
        COUNT(v)
      )
      FROM Vote v
      WHERE v.post.id = :postId
      """)
  VoteSummaryResponse summarizeByPostId(@Param("postId") UUID postId);

  boolean existsByPostId(UUID postId);

  @Query(
      """
      SELECT new com.example.sejong_voice.post.dto.PostCountRow(v.post.id, COUNT(v))
      FROM Vote v
      WHERE v.post.id IN :postIds
      GROUP BY v.post.id
      """)
  List<PostCountRow> countByPostIds(@Param("postIds") Collection<UUID> postIds);

  @Query(
      """
      SELECT v
      FROM Vote v
      JOIN FETCH v.user
      WHERE v.post.id = :postId
      ORDER BY COALESCE(v.updatedAt, v.createdAt) ASC, v.createdAt ASC
      """)
  List<Vote> findAllByPostIdWithUserOrderByVotedAtAsc(@Param("postId") UUID postId);

  @Query(
      """
    select v
    from Vote v
    join fetch v.user
    where v.post.id = :postId and v.choice = :choice
    ORDER BY COALESCE(v.updatedAt, v.createdAt) ASC, v.createdAt ASC
""")
  List<Vote> findAllByPostIdAndChoiceWithUserOrderByVotedAtTimeAsc(UUID postId, VoteChoice choice);
}
