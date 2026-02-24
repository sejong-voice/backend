package com.example.sejong_voice.comment;

import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.comment.dto.CommentResponse;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.exception.CommentErrorCode;
import com.example.sejong_voice.comment.repository.CommentRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import java.util.List;
import java.util.UUID;
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
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  @Transactional
  public UUID create(
      UUID userId,
      UUID postId,
      UUID parentId,
      String content
  ) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));


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
        .build();

    Comment saved = commentRepository.save(comment);
    log.info("댓글 생성: commentId={}, userId={}, postId={}", saved.getId(), userId, postId);

    return saved.getId();
  }

  public List<CommentResponse> getByPostId(UUID postId) {
    List<Comment> commentList = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    return commentList.stream()
        .map(CommentResponse::from)
        .toList();
  }

  @Transactional
  public void delete(UUID userId, UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

    if (!comment.getUser().getId().equals(userId)) {
      throw new BusinessException(CommentErrorCode.ACCESS_DENIED);
    }

    comment.delete();
    log.info("댓글 삭제: commentId={}, userId={}", commentId, userId);
  }
}
