package com.example.sejong_voice.comment;

import com.example.sejong_voice.comment.dto.CommentPageResponse;
import com.example.sejong_voice.comment.dto.CommentResponse;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.comment.exception.CommentErrorCode;
import com.example.sejong_voice.comment.repository.CommentAuthorProjection;
import com.example.sejong_voice.comment.repository.CommentRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.dto.PostCountRow;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentResponseAssembler commentResponseAssembler;
  private final Clock clock;

  @Transactional
  public UUID create(User user, Post post, UUID rootCommentId, String content) {
    Comment rootComment = null;
    if (rootCommentId != null) {
      rootComment =
          commentRepository
              .findById(rootCommentId)
              .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

      if (rootComment.getRootComment() != null) {
        throw new BusinessException(CommentErrorCode.ROOT_COMMENT_REQUIRED);
      }

      if (!rootComment.getPost().getId().equals(post.getId())) {
        throw new BusinessException(CommentErrorCode.ROOT_COMMENT_POST_MISMATCH);
      }

      if (rootComment.getStatus() != CommentStatus.ACTIVE) {
        throw new BusinessException(CommentErrorCode.ROOT_COMMENT_NOT_ACTIVE);
      }
    }

    Comment comment =
        Comment.builder()
            .user(user)
            .post(post)
            .rootComment(rootComment)
            .content(content)
            .createdAt(Instant.now(clock))
            .build();

    UUID id = commentRepository.save(comment).getId();
    log.info("댓글 생성: commentId={}", id);

    return id;
  }

  public CommentPageResponse getAllByPost(Post post, UUID currentUserId, Pageable pageable) {
    UUID postId = post.getId();
    UUID postAuthorId = post.getUser().getId();

    Page<Comment> rootPage =
        commentRepository.findDisplayableRootCommentsByPostId(postId, pageable);

    List<UUID> rootCommentIds = rootPage.getContent().stream().map(Comment::getId).toList();

    List<Comment> replies =
        rootCommentIds.isEmpty()
            ? List.of()
            : commentRepository.findActiveRepliesByRootCommentIds(rootCommentIds);

    Map<UUID, Integer> anonymousNumberByUserId =
        createAnonymousNumberByUserId(postId, postAuthorId);

    List<CommentResponse> responses =
        commentResponseAssembler.assemble(
            rootPage.getContent(), replies, currentUserId, postAuthorId, anonymousNumberByUserId);

    long activeCommentCount = commentRepository.countActiveByPostId(postId);

    return CommentPageResponse.of(
        responses,
        pageable.getPageNumber(),
        pageable.getPageSize(),
        rootPage.getTotalElements(),
        activeCommentCount);
  }

  private Map<UUID, Integer> createAnonymousNumberByUserId(UUID postId, UUID postAuthorId) {
    List<CommentAuthorProjection> authors =
        commentRepository.findCommentAuthorsByPostIdOrderByCreatedAt(postId);
    Map<UUID, Integer> anonymousNumberByUserId = new LinkedHashMap<>();

    for (CommentAuthorProjection author : authors) {
      UUID authorId = author.userId();

      if (authorId.equals(postAuthorId) || author.role() == UserRole.ADMIN) {
        continue;
      }

      anonymousNumberByUserId.computeIfAbsent(
          authorId, ignored -> anonymousNumberByUserId.size() + 1);
    }

    return anonymousNumberByUserId;
  }

  public Map<UUID, Long> countActiveByPostIds(Collection<UUID> postIds) {
    if (postIds.isEmpty()) {
      return Map.of();
    }

    return commentRepository.countActiveByPostIds(postIds).stream()
        .collect(Collectors.toMap(PostCountRow::postId, PostCountRow::count));
  }

  @Transactional
  public void delete(UUID userId, UUID commentId) {
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

    if (!comment.getUser().getId().equals(userId)) {
      throw new BusinessException(CommentErrorCode.ACCESS_DENIED);
    }

    comment.delete(Instant.now(clock));
    log.info("댓글 삭제: commentId={}, userId={}", commentId, userId);
  }

  @Transactional
  public void deleteAllByPostId(UUID postId) {
    List<Comment> comments = commentRepository.findAllActiveByPostId(postId);
    Instant now = Instant.now(clock);
    comments.forEach(comment -> comment.delete(now));
    log.info("게시글 댓글 삭제 처리: postId={}, commentCount={}", postId, comments.size());
  }
}
