package com.example.sejong_voice.vote;

import com.example.sejong_voice.post.dto.PostCountRow;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.Vote;
import com.example.sejong_voice.vote.entity.VoteChoice;
import com.example.sejong_voice.vote.repository.VoteRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

  private final VoteRepository voteRepository;

  /**
   * 투표하기 (Upsert: 없으면 생성, 있으면 수정)
   */
  @Transactional
  public void cast(Post post, User user, VoteChoice choice) {
    voteRepository.findByPostIdAndUserId(post.getId(), user.getId())
        .ifPresentOrElse(
            // 1. 이미 투표함 -> 선택 변경
            vote -> vote.updateChoice(choice),
            // 2. 투표 안 함 -> 새로 생성
            () -> create(post, user, choice)
        );

    log.info("투표: postId={}, userId={}, choice={}", post.getId(), user.getId(), choice);
  }

  private void create(Post post, User user, VoteChoice choice) {
    Vote vote = Vote.builder()
        .post(post)
        .user(user)
        .choice(choice)
        .build();

    voteRepository.save(vote);
  }

  /**
   * 투표 현황 조회 (통계)
   */
  public VoteSummaryResponse getSummary(UUID postId) {
    long agreeCount = countAgree(postId);
    long disagreeCount = countDisagree(postId);

    log.info("투표 요약: postId={}, agreeCount={}, disagreeCount={}", postId, agreeCount, disagreeCount);

    return VoteSummaryResponse.of(agreeCount, disagreeCount);
  }

  public long countAgree(UUID postId) {
    return voteRepository.countByPostIdAndChoice(postId, VoteChoice.AGREE);
  }

  public long countDisagree(UUID postId) {
    return voteRepository.countByPostIdAndChoice(postId, VoteChoice.DISAGREE);
  }

  public Map<UUID, Long> countByPostIds(Collection<UUID> postIds) {
    if (postIds.isEmpty()) {
      return Map.of();
    }

    return voteRepository.countByPostIds(postIds).stream()
        .collect(Collectors.toMap(
            PostCountRow::postId,
            PostCountRow::count
        ));
  }

  public boolean existsByPostId(UUID postId) {
    return voteRepository.existsByPostId(postId);
  }
}