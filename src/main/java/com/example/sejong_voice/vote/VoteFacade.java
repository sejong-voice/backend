package com.example.sejong_voice.vote;

import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.PostService;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.vote.dto.VoteAgreeVoterResponse;
import com.example.sejong_voice.vote.dto.VoteStatisticsResponse;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.VoteChoice;
import com.example.sejong_voice.vote.exception.VoteErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VoteFacade {

  private final VoteService voteService;
  private final UserService userService;
  private final PostService postService;
  private final CouncilService councilService;
  private final Clock clock;

  @Transactional
  public void cast(UUID postId, UUID userId, VoteChoice choice) {
    User user = userService.getById(userId);
    if (user.getRole() != UserRole.STUDENT) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    Post post = postService.getByIdForUpdate(postId);
    if (!councilService.hasAccessToCouncil(user, post.getCouncil())) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    if (post.getStatus() != PostStatus.VOTING
        || !Instant.now(clock).isBefore(post.getVotingEndAt())) {
      throw new BusinessException(VoteErrorCode.VOTE_CLOSED);
    }

    voteService.cast(post, user, choice);
  }

  @Transactional(readOnly = true)
  public VoteSummaryResponse getSummary(UUID postId) {
    Post post = postService.get(postId);
    return voteService.getSummary(post);
  }

  @Transactional(readOnly = true)
  public VoteStatisticsResponse getStatistics(UUID postId, UUID userId) {
    Post post = getPostForVoteResult(postId, userId);
    return voteService.getStatistics(post);
  }

  @Transactional(readOnly = true)
  public List<VoteAgreeVoterResponse> getAgreeVoters(UUID postId, UUID userId) {
    Post post = getPostForVoteResult(postId, userId);
    return voteService.getAgreeVoters(post);
  }

  private Post getPostForVoteResult(UUID postId, UUID userId) {
    Post post = postService.get(postId);
    User user = userService.getById(userId);

    boolean isSuper = user.getRole() == UserRole.SUPER;
    boolean isCouncilAdmin = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isSuper && !isCouncilAdmin) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    if (post.getStatus() != PostStatus.APPROVED
        && post.getStatus() != PostStatus.COMPLETED
        && post.getStatus() != PostStatus.REJECTED) {
      throw new BusinessException(PostErrorCode.VOTE_DETAILS_NOT_AVAILABLE);
    }
    return post;
  }
}
