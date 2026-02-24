package com.example.sejong_voice.comment;

import com.example.sejong_voice.comment.dto.CommentResponse;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.comment.exception.CommentErrorCode;
import com.example.sejong_voice.comment.repository.CommentRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @InjectMocks
  private CommentService commentService;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private PostRepository postRepository;

  @Mock
  private UserRepository userRepository;

  @Nested
  @DisplayName("댓글 생성")
  class CreateComment {

    @Test
    @DisplayName("성공: 루트 댓글 생성")
    void create_Success() {
      // given
      UUID userId = UUID.randomUUID();
      UUID postId = UUID.randomUUID();
      String content = "댓글 내용";

      User user = mock(User.class);
      Post post = mock(Post.class);
      Comment savedComment = mock(Comment.class);
      UUID commentId = UUID.randomUUID();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(postRepository.findById(postId)).thenReturn(Optional.of(post));
      when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
      when(savedComment.getId()).thenReturn(commentId);

      // when
      UUID result = commentService.create(userId, postId, null, content);

      // then
      assertThat(result).isEqualTo(commentId);
      verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("성공: 대댓글 생성")
    void createReply_Success() {
      // given
      UUID userId = UUID.randomUUID();
      UUID postId = UUID.randomUUID();
      UUID parentId = UUID.randomUUID();
      String content = "대댓글 내용";

      User user = mock(User.class);
      Post post = mock(Post.class);
      Comment parentComment = mock(Comment.class);
      Comment savedComment = mock(Comment.class);
      UUID commentId = UUID.randomUUID();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(postRepository.findById(postId)).thenReturn(Optional.of(post));
      when(commentRepository.findById(parentId)).thenReturn(Optional.of(parentComment));
      when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
      when(savedComment.getId()).thenReturn(commentId);

      // when
      UUID result = commentService.create(userId, postId, parentId, content);

      // then
      assertThat(result).isEqualTo(commentId);
      verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("실패: 유저를 찾을 수 없음")
    void create_Fail_UserNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.create(userId, UUID.randomUUID(), null, "content"))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 게시글을 찾을 수 없음")
    void create_Fail_PostNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      UUID postId = UUID.randomUUID();
      when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
      when(postRepository.findById(postId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.create(userId, postId, null, "content"))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.POST_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("댓글 조회")
  class GetComments {

    @Test
    @DisplayName("성공: 게시글의 댓글 목록 조회")
    void getByPostId_Success() {
      // given
      UUID postId = UUID.randomUUID();
      User user = mock(User.class);
      when(user.getName()).thenReturn("작성자");

      Comment comment = Comment.builder()
          .user(user)
          .post(mock(Post.class))
          .content("내용")
          .build();

      when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId)).thenReturn(List.of(comment));

      // when
      List<CommentResponse> result = commentService.getByPostId(postId);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).userName()).isEqualTo("작성자");
    }
  }

  @Nested
  @DisplayName("댓글 삭제")
  class DeleteComment {

    @Test
    @DisplayName("성공: 댓글 삭제")
    void delete_Success() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      User user = mock(User.class);
      Comment comment = spy(Comment.builder()
          .user(user)
          .post(mock(Post.class))
          .content("내용")
          .build());

      when(user.getId()).thenReturn(userId);
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

      // when
      commentService.delete(userId, commentId);

      // then
      assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
      verify(comment).delete();
    }

    @Test
    @DisplayName("실패: 본인 댓글이 아님")
    void delete_Fail_AccessDenied() {
      // given
      UUID userId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      
      User owner = mock(User.class);
      when(owner.getId()).thenReturn(otherUserId);
      
      Comment comment = Comment.builder()
          .user(owner)
          .post(mock(Post.class))
          .content("내용")
          .build();

      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

      // when & then
      assertThatThrownBy(() -> commentService.delete(userId, commentId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.ACCESS_DENIED);
    }
  }
}
