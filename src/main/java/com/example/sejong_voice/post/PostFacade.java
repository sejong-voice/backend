package com.example.sejong_voice.post;

import com.example.sejong_voice.comment.CommentService;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.ImageFacade;
import com.example.sejong_voice.image.ImageService;
import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.notification.NotificationService;
import com.example.sejong_voice.post.dto.PostDetailResponse;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.dto.PostStatementResponse;
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
    imageService.attachImagesToPost(user, post, imageIds);

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

    Map<UUID, List<PostImageResponse>> imagesByPostId = imageFacade.getActivePostImagesByPostIds(
        postIds);
    Map<UUID, Long> voteCountByPostId = voteService.countByPostIds(postIds);
    Map<UUID, Long> commentCountByPostId = commentService.countActiveByPostIds(postIds);

    return postPage.map(response -> enrichImages(response.withCounts(
        voteCountByPostId.getOrDefault(response.id(), 0L),
        commentCountByPostId.getOrDefault(response.id(), 0L)
    ), imagesByPostId));
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

    if (!isSuper && post.getStatus() != PostStatus.VOTING) {
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

  /**
   * 게시글에 입장문을 생성합니다.
   *
   * @param postId   게시글 ID
   * @param userId   요청한 사용자 ID
   * @param finalStatus 첫 입장문 생성 시 확정할 최종 상태
   * @param content  입장문 내용
   * @param imageIds 입장문 이미지 ID 목록
   * @throws BusinessException 접근 권한이 없거나 입장문 생성 조건을 만족하지 못한 경우
   */
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
    } else if (post.getStatus() != PostStatus.COMPLETED && post.getStatus() != PostStatus.REJECTED) {
      throw new BusinessException(PostErrorCode.STATEMENT_CREATE_NOT_ALLOWED);
    }

    User user = userService.getById(userId);
    int nextSequence = (int) statementCount + 1;
    PostStatement postStatement = postStatementService.create(post, user, nextSequence, content);
    imageService.attachImagesToStatement(user, postStatement, imageIds);
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
    List<PostStatement> statements = postStatementService.getStatements(postResponse.id());
    List<UUID> statementIds = statements.stream()
        .map(PostStatement::getId)
        .toList();
    Map<UUID, List<PostImageResponse>> statementImagesById =
        imageFacade.getActiveStatementImagesByStatementIds(statementIds);
    List<PostStatementResponse> statementResponses = statements.stream()
        .map(statement -> PostStatementResponse.from(statement)
            .withImages(statementImagesById.getOrDefault(statement.getId(), List.of())))
        .toList();

    return postResponse.withImages(
        imageFacade.getActivePostImages(postResponse.id())
    ).withStatements(statementResponses);
  }

  private PostListResponse enrichImages(
      PostListResponse postResponse,
      Map<UUID, List<PostImageResponse>> imagesByPostId
  ) {
    return postResponse.withImages(imagesByPostId.getOrDefault(postResponse.id(), List.of()));
  }
}
