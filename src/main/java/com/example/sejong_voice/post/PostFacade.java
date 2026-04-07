package com.example.sejong_voice.post;

import com.example.sejong_voice.comment.CommentService;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.ImageFacade;
import com.example.sejong_voice.image.ImageService;
import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.image.entity.ImageTargetType;
import com.example.sejong_voice.notification.NotificationService;
import com.example.sejong_voice.post.dto.PostDetailResponse;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.vote.VoteService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostFacade {

  private final PostService postService;
  private final UserService userService;
  private final CouncilService councilService;
  private final VoteService voteService;
  private final NotificationService notificationService;
  private final ImageService imageService;
  private final ImageFacade imageFacade;
  private final CommentService commentService;

  @Transactional
  public UUID create(
      UUID userId,
      String title,
      String content,
      UUID councilId,
      PostVotingDuration votingDuration,
      List<UUID> imageIds
  ) {
    User user = userService.getById(userId);
    Council council = councilService.getById(councilId);
    Post post = postService.create(user, council, title, content, votingDuration);
    imageService.attachImagesToPost(user, post, imageIds, ImageTargetType.POST_CONTENT);

    return post.getId();
  }

  @Transactional
  public PostDetailResponse getPost(UUID postId) {
    return enrichImages(postService.get(postId));
  }

  @Transactional(readOnly = true)
  public Page<PostListResponse> getList(PostSearchCondition condition, Pageable pageable) {
    if (Boolean.TRUE.equals(condition.assignedToMe())) {
      UUID currentUserId = condition.currentUserId();

      if (!councilService.hasManagedCouncil(currentUserId)) {
        throw new BusinessException(PostErrorCode.ACCESS_DENIED);
      }
    }

    Page<PostListResponse> postPage = postService.getList(condition, pageable);
    List<UUID> postIds = postPage.getContent().stream()
        .map(PostListResponse::id)
        .toList();

    Map<UUID, List<PostImageResponse>> imagesByPostId = imageFacade.getActiveImagesByPostIds(
        postIds,
        ImageTargetType.POST_CONTENT
    );
    Map<UUID, Long> voteCountByPostId = voteService.countByPostIds(postIds);
    Map<UUID, Long> commentCountByPostId = commentService.countActiveByPostIds(postIds);

    return postPage.map(response -> enrichImages(response.withCounts(
        voteCountByPostId.getOrDefault(response.id(), 0L),
        commentCountByPostId.getOrDefault(response.id(), 0L)
    ), imagesByPostId));
  }

  // TODO: 삭제
  @Transactional
  public PostDetailResponse update(
      UUID postId,
      String title,
      String content,
      List<UUID> imageIds,
      UUID userId
  ) {
    postService.update(postId, title, content, userId);

    User user = userService.getById(userId);
    Post post = postService.getById(postId);
    imageService.syncImagesForPost(user, post, imageIds, ImageTargetType.POST_CONTENT);

    return enrichImages(PostDetailResponse.from(post));
  }

  @Transactional
  public void delete(UUID postId, UUID userId) {
    Post post = postService.getById(postId);
    User user = userService.getById(userId);

    boolean isAuthor = post.getUser().getId().equals(userId);
    boolean isSuper = user.getRole() == UserRole.SUPER;
    boolean isCouncilAdmin = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isAuthor && !isSuper && !isCouncilAdmin) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    if (post.getStatus() != PostStatus.VOTING) {
      throw new BusinessException(PostErrorCode.POST_CANNOT_DELETE_VOTING_ENDED);
    }

    commentService.deleteAllByPostId(postId);
    imageService.deleteAllByPostId(postId);
    postService.delete(postId);
  }

  @Transactional
  public void closeEarly(UUID postId, UUID userId) {
    Post post = postService.getById(postId);
    boolean isMember = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isMember) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    processClosing(postId);
  }

  @Transactional
  public void updateResult(
      UUID postId,
      UUID userId,
      PostStatus status,
      String resultContent,
      List<UUID> imageIds
  ) {
    Post post = postService.getById(postId);
    boolean isMember = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isMember) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    if (status != PostStatus.COMPLETED && status != PostStatus.REJECTED) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    validateResultUpdatableStatus(post.getStatus());

    User user = userService.getById(userId);
    imageService.syncImagesForPost(user, post, imageIds, ImageTargetType.POST_RESULT);
    postService.updateResult(postId, status, resultContent);
  }

  @Transactional
  public void processClosing(UUID postId) {
    Post post = postService.getById(postId);
    PostStatus beforeStatus = post.getStatus();

    long agreeCount = voteService.countAgree(postId);
    long totalCount = agreeCount + voteService.countDisagree(postId);

    postService.evaluateAndClose(postId, agreeCount, totalCount);

    PostStatus afterStatus = post.getStatus();
    if (beforeStatus != PostStatus.APPROVED && afterStatus == PostStatus.APPROVED) {
      notificationService.createPostApprovedNotifications(post);
    }
  }

  private PostDetailResponse enrichImages(PostDetailResponse postResponse) {
    return postResponse.withImages(
        imageFacade.getActiveImages(postResponse.id(), ImageTargetType.POST_CONTENT)
    ).withResultImages(
        imageFacade.getActiveImages(postResponse.id(), ImageTargetType.POST_RESULT)
    );
  }

  private PostListResponse enrichImages(
      PostListResponse postResponse,
      Map<UUID, List<PostImageResponse>> imagesByPostId
  ) {
    return postResponse.withImages(imagesByPostId.getOrDefault(postResponse.id(), List.of()));
  }

  private void validateResultUpdatableStatus(PostStatus currentStatus) {
    if (currentStatus != PostStatus.APPROVED
        && currentStatus != PostStatus.COMPLETED
        && currentStatus != PostStatus.REJECTED) {
      throw new BusinessException(PostErrorCode.RESULT_UPDATE_NOT_ALLOWED);
    }
  }
}
