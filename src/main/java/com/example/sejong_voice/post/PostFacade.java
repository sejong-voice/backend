package com.example.sejong_voice.post;

import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.notification.NotificationService;
import com.example.sejong_voice.post.dto.PostResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.vote.VoteService;
import com.example.sejong_voice.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostFacade {

  private final PostService postService;
  private final UserService userService;
  private final CouncilService councilService;
  private final VoteService voteService;
  private final NotificationService notificationService;

  @Transactional
  public UUID create(
      UUID userId,
      String title,
      String content,
      UUID councilId,
      PostVotingDuration votingDuration
  ) {
    User user = userService.getById(userId);
    Council council = councilService.getById(councilId);

    return postService.create(user, council, title, content, votingDuration).getId();
  }

  @Transactional
  public PostResponse getPost(UUID postId) {
    return postService.get(postId);
  }

  @Transactional(readOnly = true)
  public Page<PostResponse> getAllPosts(PostSearchCondition condition, Pageable pageable) {
    if (Boolean.TRUE.equals(condition.assignedToMe())) {
      UUID currentUserId = condition.currentUserId();

      if (!councilService.hasManagedCouncil(currentUserId)) {
        throw new BusinessException(PostErrorCode.ACCESS_DENIED);
      }
    }

    return postService.getAll(condition, pageable);
  }

  @Transactional
  public PostResponse update(UUID postId, String title, String content, UUID userId) {
    return postService.update(postId, title, content, userId);
  }

  @Transactional
  public void delete(UUID postId, UUID userId) {
    Post post = postService.getById(postId);
    User user = userService.getById(userId);

    // 1. 권한 체크
    boolean isAuthor = post.getUser().getId().equals(userId);
    boolean isSuper = user.getRole() == UserRole.SUPER;
    boolean isCouncilAdmin = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isAuthor && !isSuper && !isCouncilAdmin) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    // 2. 제약 조건 체크 (학생 작성자의 경우 투표가 1개라도 있으면 삭제 불가)
    if (isAuthor && !isSuper && !isCouncilAdmin) {
      if (voteService.existsByPostId(postId)) {
        throw new BusinessException(PostErrorCode.CANNOT_DELETE_POST_WITH_VOTES);
      }
    }

    // 3. 삭제 수행 (Soft Delete)
    postService.delete(postId);
  }

  @Transactional
  public void closeEarly(UUID postId, UUID userId) {
    Post post = postService.getById(postId);

    // 권한 체크 (해당 학생회의 운영진만 가능)
    boolean isMember = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isMember) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    processClosing(postId);
  }

  @Transactional
  public void updateResult(UUID postId, UUID userId, PostStatus status, String resultContent) {
    Post post = postService.getById(postId);

    // 권한 체크 (해당 학생회의 운영진만 가능)
    boolean isMember = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isMember) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    if (status != PostStatus.COMPLETED && status != PostStatus.REJECTED) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    postService.updateResult(postId, status, resultContent);
  }

  @Transactional
  public void processClosing(UUID postId) {
    Post post = postService.getById(postId);
    PostStatus beforeStatus = post.getStatus();

    long agreeCount = voteService.countAgree(postId);
    long totalCount = agreeCount + voteService.countDisagree(postId);

    postService.evaluateAndClose(postId, agreeCount, totalCount);

    PostStatus afterStatus = post.getStatus(); // 같은 트랜잭션 내 managed entity 기준

    // "APPROVED 상태인지"가 아니라 "APPROVED로 전환됐는지" 체크
    if (beforeStatus != PostStatus.APPROVED && afterStatus == PostStatus.APPROVED) {
      notificationService.createPostApprovedNotifications(post);
    }

  }
}
