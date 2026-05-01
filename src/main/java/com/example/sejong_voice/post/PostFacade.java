package com.example.sejong_voice.post;

import com.example.sejong_voice.category.CategoryService;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.comment.CommentService;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.ImageService;
import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.image.dto.PostImageView;
import com.example.sejong_voice.image.dto.PostStatementImageView;
import com.example.sejong_voice.notification.NotificationService;
import com.example.sejong_voice.post.dto.PostDetailResponse;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.dto.PostStatementResponse;
import com.example.sejong_voice.post.dto.PostStatementView;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.vote.VoteService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostFacade {

  private final PostService postService;
  private final PostStatementService postStatementService;
  private final CategoryService categoryService;
  private final UserService userService;
  private final CouncilService councilService;
  private final VoteService voteService;
  private final NotificationService notificationService;
  private final ImageService imageService;
  private final CommentService commentService;
  private final Clock clock;

  @Transactional
  public UUID create(
      UUID userId,
      String title,
      String content,
      UUID councilId,
      UUID categoryId,
      PostVotingDuration votingDuration,
      List<UUID> imageIds
  ) {
    User user = userService.getById(userId);
    Council council = councilService.getById(councilId);
    if (!councilService.hasAccessToCouncil(user, council)) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }
    Category category = categoryService.getById(categoryId);
    Post post = postService.create(user, council, category, title, content, votingDuration);
    imageService.attachImagesToPost(user, post, imageIds);
    return post.getId();
  }

  @Transactional
  public PostDetailResponse get(UUID postId, UUID userId) {
    Post post = postService.get(postId);
    post.incrementViewCount();

    boolean canVote = false;
    if (userId != null) {
      User user = userService.getById(userId);
      canVote = councilService.hasAccessToCouncil(user, post.getCouncil())
          && post.getStatus() == PostStatus.VOTING
          && Instant.now(clock).isBefore(post.getVotingEndAt());
    }

    long agreeCount = voteService.countAgree(postId);
    long totalCount = agreeCount + voteService.countDisagree(postId);
    boolean canCloseEarly = postService.canCloseEarly(post, agreeCount, totalCount);

    List<PostImageView> images = imageService.getPostImages(post.getId());
    List<PostImageResponse> imageResponses = images.stream()
        .map(image -> PostImageResponse.of(image.imageId(), image.imageUrl()))
        .toList();

    List<PostStatementView> statements = postStatementService.getStatements(post.getId());
    List<UUID> statementIds = statements.stream()
        .map(PostStatementView::id)
        .toList();

    Map<UUID, List<PostImageResponse>> statementImageResponsesById =
        imageService.getStatementImagesByStatementIds(statementIds)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(PostFacade::toPostImageResponse)
                    .toList()
            ));

    List<PostStatementResponse> statementResponses = statements.stream()
        .map(statement -> PostStatementResponse.from(
            statement,
            statementImageResponsesById.getOrDefault(statement.id(), List.of())
        ))
        .toList();

    return PostDetailResponse.from(
        post,
        canVote,
        canCloseEarly,
        imageResponses,
        statementResponses
    );
  }

  @Transactional(readOnly = true)
  public Page<PostListResponse> getPosts(PostSearchCondition condition, Pageable pageable) {
    if (Boolean.TRUE.equals(condition.assignedToMe())) {
      UUID currentUserId = condition.currentUserId();

      if (!councilService.hasManagedCouncil(currentUserId)) {
        throw new BusinessException(PostErrorCode.ACCESS_DENIED);
      }
    }

    Page<PostListResponse> postPage = postService.getPosts(condition, pageable);
    List<UUID> postIds = postPage.getContent().stream()
        .map(PostListResponse::id)
        .toList();

    Map<UUID, Long> voteCountByPostId = voteService.countByPostIds(postIds);
    Map<UUID, Long> commentCountByPostId = commentService.countActiveByPostIds(postIds);

    return postPage.map(response -> response.withCounts(
        voteCountByPostId.getOrDefault(response.id(), 0L),
        commentCountByPostId.getOrDefault(response.id(), 0L)
    ));
  }

  @Transactional
  public void delete(UUID postId, UUID userId) {
    Post post = postService.get(postId);
    User user = userService.getById(userId);

    boolean isAuthor = post.getUser().getId().equals(userId);
    boolean isSuper = user.getRole() == UserRole.SUPER;
    boolean isCouncilAdmin = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isAuthor && !isSuper && !isCouncilAdmin) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    if (!isSuper && post.getStatus() != PostStatus.VOTING) {
      throw new BusinessException(PostErrorCode.POST_CANNOT_DELETE_VOTING_ENDED);
    }

    commentService.deleteAllByPostId(postId);
    imageService.deleteAllByPostId(postId);
    postService.delete(postId);
  }

  @Transactional
  public void closeEarly(UUID postId, UUID userId) {
    Post post = postService.get(postId);
    boolean isMember = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isMember) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    long agreeCount = voteService.countAgree(postId);
    long totalCount = agreeCount + voteService.countDisagree(postId);

    postService.closeEarly(postId, agreeCount, totalCount);
    notificationService.createPostApprovedNotifications(post);
  }

  @Transactional
  public void createStatement(
      UUID postId,
      UUID userId,
      PostStatus finalStatus,
      String content,
      List<UUID> imageIds
  ) {
    Post post = postService.getByIdForUpdate(postId);
    boolean isMember = councilService.isAdmin(userId, post.getCouncil().getId());

    if (!isMember) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    long statementCount = postStatementService.countStatements(postId);
    if (statementCount >= 2) {
      throw new BusinessException(PostErrorCode.STATEMENT_LIMIT_EXCEEDED);
    }

    if (statementCount == 0) {
      if (post.getStatus() != PostStatus.APPROVED) {
        throw new BusinessException(PostErrorCode.STATEMENT_CREATE_NOT_ALLOWED);
      }

      if (finalStatus == null) {
        throw new BusinessException(PostErrorCode.FINAL_STATUS_REQUIRED);
      }

      if (finalStatus != PostStatus.COMPLETED && finalStatus != PostStatus.REJECTED) {
        throw new BusinessException(PostErrorCode.INVALID_FINAL_STATUS);
      }
      post.updateStatus(finalStatus);
    } else if (finalStatus != null) {
      throw new BusinessException(PostErrorCode.FINAL_STATUS_NOT_ALLOWED);
    } else if (post.getStatus() != PostStatus.COMPLETED
        && post.getStatus() != PostStatus.REJECTED) {
      throw new BusinessException(PostErrorCode.STATEMENT_CREATE_NOT_ALLOWED);
    }

    User user = userService.getById(userId);
    int nextSequence = (int) statementCount + 1;
    PostStatement postStatement = postStatementService.create(post, user, nextSequence, content);
    imageService.attachImagesToStatement(user, postStatement, imageIds);
  }

  @Transactional
  public void processClosing(UUID postId) {
    Post post = postService.get(postId);
    PostStatus beforeStatus = post.getStatus();

    long agreeCount = voteService.countAgree(postId);
    long totalCount = agreeCount + voteService.countDisagree(postId);

    postService.close(postId, agreeCount, totalCount);

    PostStatus afterStatus = post.getStatus();
    if (beforeStatus != PostStatus.APPROVED && afterStatus == PostStatus.APPROVED) {
      notificationService.createPostApprovedNotifications(post);
    }
  }

  private static PostImageResponse toPostImageResponse(PostStatementImageView image) {
    return new PostImageResponse(image.imageId(), image.imageUrl());
  }
}
