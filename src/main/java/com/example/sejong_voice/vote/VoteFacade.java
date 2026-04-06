package com.example.sejong_voice.vote;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.PostService;
import com.example.sejong_voice.vote.exception.VoteErrorCode;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.vote.dto.VoteDetailResponse;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.VoteChoice;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.exception.PostErrorCode;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    Post post = postService.getById(postId);

    if (post.getStatus() != PostStatus.VOTING || !Instant.now(clock).isBefore(post.getVotingEndAt())) {
      throw new BusinessException(VoteErrorCode.VOTE_CLOSED);
    }

    voteService.cast(post, user, choice);
  }

  @Transactional(readOnly = true)
  public VoteSummaryResponse getSummary(UUID postId) {
    return voteService.getSummary(postId);
  }

  @Transactional(readOnly = true)
  public VoteDetailResponse getDetails(UUID postId, UUID userId) {
    Post post = postService.getById(postId);
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

    return voteService.getDetails(post);
  }
}
