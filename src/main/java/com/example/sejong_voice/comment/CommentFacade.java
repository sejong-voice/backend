package com.example.sejong_voice.comment;

import com.example.sejong_voice.comment.dto.CommentPageResponse;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.PostService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommentFacade {

  private final CommentService commentService;
  private final UserService userService;
  private final PostService postService;

  @Transactional
  public UUID create(UUID userId, UUID postId, UUID rootCommentId, String content) {
    User user = userService.getById(userId);
    Post post = postService.getById(postId);
    
    return commentService.create(user, post, rootCommentId, content);
  }

  @Transactional(readOnly = true)
  public CommentPageResponse getAllByPostId(UUID postId, UUID currentUserId, Pageable pageable) {
    Post post = postService.getById(postId);

    return commentService.getAllByPost(post, currentUserId, pageable);
  }

  @Transactional
  public void delete(UUID userId, UUID commentId) {
    commentService.delete(userId, commentId);
  }
}
