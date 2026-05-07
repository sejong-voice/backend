package com.example.sejong_voice.vote;

import com.example.sejong_voice.post.dto.PostCountRow;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.vote.dto.VoteAgreeVoterResponse;
import com.example.sejong_voice.vote.dto.VoteDepartmentDistributionResponse;
import com.example.sejong_voice.vote.dto.VoteStatisticsResponse;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.Vote;
import com.example.sejong_voice.vote.entity.VoteChoice;
import com.example.sejong_voice.vote.repository.VoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

  private final VoteRepository voteRepository;

  /** 투표하기 (Upsert: 없으면 생성, 있으면 수정) */
  @Transactional
  public void cast(Post post, User user, VoteChoice choice) {
    Vote existingVote = voteRepository.findByPostIdAndUserId(post.getId(), user.getId()).orElse(null);

    if (existingVote != null) {
      existingVote.updateChoice(choice);
      log.info("투표: postId={}, userId={}, choice={}", post.getId(), user.getId(), choice);
      return;
    }

    try {
      create(post, user, choice);
    } catch (DataIntegrityViolationException e) {
      // Concurrent first-vote requests can race on the (post_id, user_id) unique key.
      // When that happens, reload the just-created row and treat this request as an update.
      Vote concurrentVote =
          voteRepository
              .findByPostIdAndUserId(post.getId(), user.getId())
              .orElseThrow(() -> e);
      concurrentVote.updateChoice(choice);
    }

    log.info("투표: postId={}, userId={}, choice={}", post.getId(), user.getId(), choice);
  }

  private void create(Post post, User user, VoteChoice choice) {
    Vote vote = Vote.builder().post(post).user(user).choice(choice).build();

    voteRepository.save(vote);
  }

  /** 투표 현황 요약 조회 */
  public VoteSummaryResponse getSummary(UUID postId) {
    VoteSummaryResponse summary = voteRepository.summarizeByPostId(postId);

    log.info(
        "투표 요약: postId={}, agreeCount={}, disagreeCount={}",
        postId,
        summary.agreeCount(),
        summary.disagreeCount());

    return summary;
  }

  public VoteStatisticsResponse getStatistics(Post post) {
    List<Vote> votes = voteRepository.findAllByPostIdWithUserOrderByVotedAtAsc(post.getId());
    long agreeCount = votes.stream().filter(vote -> vote.getChoice() == VoteChoice.AGREE).count();
    long disagreeCount =
        votes.stream().filter(vote -> vote.getChoice() == VoteChoice.DISAGREE).count();

    List<VoteDepartmentDistributionResponse> departmentDistributions =
        votes.stream()
            .collect(Collectors.groupingBy(vote -> vote.getUser().getDepartment()))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  long departmentAgreeCount =
                      entry.getValue().stream()
                          .filter(vote -> vote.getChoice() == VoteChoice.AGREE)
                          .count();
                  long departmentDisagreeCount =
                      entry.getValue().stream()
                          .filter(vote -> vote.getChoice() == VoteChoice.DISAGREE)
                          .count();

                  return new VoteDepartmentDistributionResponse(
                      entry.getKey(),
                      departmentAgreeCount,
                      departmentDisagreeCount,
                      entry.getValue().size());
                })
            .sorted(
                Comparator.comparingLong(VoteDepartmentDistributionResponse::totalCount)
                    .reversed()
                    .thenComparing(VoteDepartmentDistributionResponse::department))
            .toList();

    BigDecimal agreeRatio =
        votes.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(agreeCount)
                .divide(BigDecimal.valueOf(votes.size()), 4, RoundingMode.HALF_UP);

    log.info("투표 통계 조회: postId={}, totalCount={}", post.getId(), votes.size());

    return VoteStatisticsResponse.of(
        agreeCount, disagreeCount, votes.size(), agreeRatio, departmentDistributions);
  }

  public List<VoteAgreeVoterResponse> getAgreeVoters(Post post) {
    log.info("투표 찬성자 명단 조회: postId={}", post.getId());
    return voteRepository
        .findAllByPostIdAndChoiceWithUserOrderByVotedAtTimeAsc(post.getId(), VoteChoice.AGREE)
        .stream()
        .map(VoteAgreeVoterResponse::from)
        .toList();
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
        .collect(Collectors.toMap(PostCountRow::postId, PostCountRow::count));
  }
}
