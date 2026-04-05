package com.example.sejong_voice.comment;

import com.example.sejong_voice.comment.dto.CommentResponse;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.exception.CommentErrorCode;
import com.example.sejong_voice.comment.repository.CommentRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.dto.PostCountRow;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final Clock clock;

  @Transactional
  public UUID create(
      User user,
      Post post,
      UUID parentId,
      String content
  ) {
    Comment parent = null;
    if (parentId != null) {
      parent = commentRepository.findById(parentId)
          .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    Comment comment = Comment.builder()
        .user(user)
        .post(post)
        .parent(parent)
        .content(content)
        .createdAt(Instant.now(clock))
        .build();

    Comment saved = commentRepository.save(comment);
    log.info("댓글 생성: commentId={}, userId={}, postId={}", saved.getId(), user.getId(), post.getId());

    return saved.getId();
  }

  public List<CommentResponse> getByPostId(UUID postId, UUID currentUserId) {
    List<Comment> commentList = commentRepository.findAllActiveByPostIdWithUserOrderByCreatedAtAsc(postId);
    return commentList.stream()
        .filter(comment -> comment.getParent() == null)
        .map(comment -> CommentResponse.from(comment, currentUserId))
        .toList();
  }

  public Map<UUID, Long> countActiveByPostIds(Collection<UUID> postIds) {
    if (postIds.isEmpty()) {
      return Map.of();
    }

    return commentRepository.countActiveByPostIds(postIds).stream()
        .collect(Collectors.toMap(
            PostCountRow::postId,
            PostCountRow::count
        ));
  }

  @Transactional
  public void delete(UUID userId, UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
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
