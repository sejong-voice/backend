package com.example.sejong_voice.post.vote;

import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.post.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.post.vote.entity.Vote;
import com.example.sejong_voice.post.vote.entity.VoteChoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostVoteService {

  private final PostVoteRepository voteRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * 투표하기 (Upsert: 없으면 생성, 있으면 수정)
   */
  @Transactional
  public void cast(UUID postId, UUID userId, VoteChoice choice) {
    voteRepository.findByPostIdAndUserId(postId, userId)
        .ifPresentOrElse(
            // 1. 이미 투표함 -> 선택 변경
            vote -> vote.updateChoice(choice),
            // 2. 투표 안 함 -> 새로 생성
            () -> create(postId, userId, choice)
        );

    log.info("투표: postId={}, userId={}, choice={}", postId, userId, choice);
  }

  private void create(UUID postId, UUID userId, VoteChoice choice) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

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
    long agreeCount = voteRepository.countByPostIdAndChoice(postId, VoteChoice.AGREE);
    long disagreeCount = voteRepository.countByPostIdAndChoice(postId, VoteChoice.DISAGREE);

    log.info("투표 요약: postId={}, agreeCount={}, disagreeCount={}", postId, agreeCount, disagreeCount);

    return VoteSummaryResponse.of(agreeCount, disagreeCount);
  }
}