package com.example.sejong_voice.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.comment.dto.CommentPageResponse;
import com.example.sejong_voice.comment.dto.CommentResponse;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.comment.exception.CommentErrorCode;
import com.example.sejong_voice.comment.repository.CommentRepository;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.dto.PostCountRow;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock private CommentRepository commentRepository;
  @Mock private CommentResponseAssembler commentResponseAssembler;

  private CommentService commentService;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    commentService = new CommentService(commentRepository, commentResponseAssembler, fixedClock);
  }

  @Test
  void create_루트댓글이면_정상생성한다() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), user);

    when(commentRepository.save(any(Comment.class)))
        .thenAnswer(
            invocation -> {
              Comment comment = invocation.getArgument(0);
              ReflectionTestUtils.setField(
                  comment, "id", UUID.fromString("00000000-0000-0000-0000-000000000111"));
              return comment;
            });

    UUID result = commentService.create(user, post, null, "댓글");

    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000111"), result);
    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Comment saved = captor.getValue();
    assertSame(user, saved.getUser());
    assertSame(post, saved.getPost());
    assertNull(saved.getRootComment());
    assertEquals("댓글", saved.getContent());
    assertEquals(Instant.now(fixedClock), saved.getCreatedAt());
  }

  @Test
  void create_루트댓글이_없으면_COMMENT_NOT_FOUND() {
    User user = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), user);
    UUID rootCommentId = UUID.randomUUID();
    when(commentRepository.findById(rootCommentId)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> commentService.create(user, post, rootCommentId, "대댓글"));

    assertSame(CommentErrorCode.COMMENT_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void create_대댓글의_대댓글이면_ROOT_COMMENT_REQUIRED() {
    User author = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), author);
    Comment rootReply =
        buildComment(
            UUID.randomUUID(),
            post,
            author,
            buildComment(UUID.randomUUID(), post, author, null, "루트"),
            "대댓글");

    when(commentRepository.findById(rootReply.getId())).thenReturn(Optional.of(rootReply));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> commentService.create(author, post, rootReply.getId(), "대댓글의 대댓글"));

    assertSame(CommentErrorCode.ROOT_COMMENT_REQUIRED, exception.getErrorCode());
  }

  @Test
  void create_다른게시글의_루트댓글이면_ROOT_COMMENT_POST_MISMATCH() {
    User author = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), author);
    Post otherPost = buildPost(UUID.randomUUID(), author);
    Comment rootComment = buildComment(UUID.randomUUID(), otherPost, author, null, "루트");

    when(commentRepository.findById(rootComment.getId())).thenReturn(Optional.of(rootComment));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> commentService.create(author, post, rootComment.getId(), "대댓글"));

    assertSame(CommentErrorCode.ROOT_COMMENT_POST_MISMATCH, exception.getErrorCode());
  }

  @Test
  void create_비활성_루트댓글이면_ROOT_COMMENT_NOT_ACTIVE() {
    User author = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), author);
    Comment rootComment = buildComment(UUID.randomUUID(), post, author, null, "루트");
    rootComment.delete(Instant.now(fixedClock));

    when(commentRepository.findById(rootComment.getId())).thenReturn(Optional.of(rootComment));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> commentService.create(author, post, rootComment.getId(), "대댓글"));

    assertSame(CommentErrorCode.ROOT_COMMENT_NOT_ACTIVE, exception.getErrorCode());
  }

  @Test
  void getAllByPost_루트댓글과_대댓글과_익명번호를_조합해_페이지응답을_반환한다() {
    UUID currentUserId = UUID.randomUUID();
    User postAuthor = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), postAuthor);
    User firstCommentUser = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    User secondCommentUser = buildUser(UUID.randomUUID(), "전자정보공학과");
    Comment rootComment = buildComment(UUID.randomUUID(), post, firstCommentUser, null, "루트");
    Comment reply = buildComment(UUID.randomUUID(), post, secondCommentUser, rootComment, "대댓글");
    Pageable pageable = PageRequest.of(0, 10);
    Page<Comment> rootPage = new PageImpl<>(List.of(rootComment), pageable, 1);
    List<CommentResponse> assembled =
        List.of(
            new CommentResponse(
                rootComment.getId(),
                1,
                "루트",
                CommentStatus.ACTIVE,
                false,
                false,
                rootComment.getCreatedAt(),
                List.of()));

    when(commentRepository.findDisplayableRootCommentsByPostId(post.getId(), pageable))
        .thenReturn(rootPage);
    when(commentRepository.findActiveRepliesByRootCommentIds(List.of(rootComment.getId())))
        .thenReturn(List.of(reply));
    when(commentRepository.findCommentAuthorIdsByPostIdOrderByFirstCommentAt(post.getId()))
        .thenReturn(List.of(firstCommentUser.getId(), secondCommentUser.getId()));
    when(commentRepository.countActiveByPostId(post.getId())).thenReturn(2L);
    when(commentResponseAssembler.assemble(
            List.of(rootComment),
            List.of(reply),
            currentUserId,
            postAuthor.getId(),
            Map.of(firstCommentUser.getId(), 1, secondCommentUser.getId(), 2)))
        .thenReturn(assembled);

    CommentPageResponse result = commentService.getAllByPost(post, currentUserId, pageable);

    assertEquals(assembled, result.content());
    assertEquals(0, result.page());
    assertEquals(10, result.size());
    assertEquals(1, result.numberOfElements());
    assertEquals(1L, result.displayableRootCommentCount());
    assertEquals(2L, result.activeCommentCount());
    assertEquals(1, result.totalPages());
    assertTrue(result.first());
    assertTrue(result.last());
  }

  @Test
  void countActiveByPostIds_빈목록이면_빈맵을_반환한다() {
    Map<UUID, Long> result = commentService.countActiveByPostIds(List.of());

    assertTrue(result.isEmpty());
    verify(commentRepository, never()).countActiveByPostIds(any());
  }

  @Test
  void countActiveByPostIds_집계결과를_맵으로_반환한다() {
    UUID postId1 = UUID.randomUUID();
    UUID postId2 = UUID.randomUUID();
    when(commentRepository.countActiveByPostIds(List.of(postId1, postId2)))
        .thenReturn(List.of(new PostCountRow(postId1, 3L), new PostCountRow(postId2, 5L)));

    Map<UUID, Long> result = commentService.countActiveByPostIds(List.of(postId1, postId2));

    assertEquals(3L, result.get(postId1));
    assertEquals(5L, result.get(postId2));
  }

  @Test
  void delete_댓글작성자가_아니면_ACCESS_DENIED() {
    UUID actorId = UUID.randomUUID();
    User author = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), author);
    Comment comment = buildComment(UUID.randomUUID(), post, author, null, "댓글");
    when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> commentService.delete(actorId, comment.getId()));

    assertSame(CommentErrorCode.ACCESS_DENIED, exception.getErrorCode());
    assertEquals(CommentStatus.ACTIVE, comment.getStatus());
  }

  @Test
  void delete_댓글작성자면_삭제처리한다() {
    User author = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), author);
    Comment comment = buildComment(UUID.randomUUID(), post, author, null, "댓글");
    when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

    commentService.delete(author.getId(), comment.getId());

    assertEquals(CommentStatus.DELETED, comment.getStatus());
    assertEquals(Instant.now(fixedClock), comment.getDeletedAt());
  }

  @Test
  void deleteAllByPostId_활성댓글들을_모두_삭제처리한다() {
    User author = buildUser(UUID.randomUUID(), "컴퓨터공학과");
    Post post = buildPost(UUID.randomUUID(), author);
    Comment comment1 = buildComment(UUID.randomUUID(), post, author, null, "댓글1");
    Comment comment2 = buildComment(UUID.randomUUID(), post, author, null, "댓글2");
    when(commentRepository.findAllActiveByPostId(post.getId()))
        .thenReturn(List.of(comment1, comment2));

    commentService.deleteAllByPostId(post.getId());

    assertEquals(CommentStatus.DELETED, comment1.getStatus());
    assertEquals(CommentStatus.DELETED, comment2.getStatus());
    assertEquals(Instant.now(fixedClock), comment1.getDeletedAt());
    assertEquals(Instant.now(fixedClock), comment2.getDeletedAt());
  }

  private User buildUser(UUID id, String affiliationName) {
    Affiliation affiliation = Affiliation.builder().name(affiliationName).build();

    User user =
        User.builder()
            .studentNo("20201234")
            .name("홍길동")
            .affiliation(affiliation)
            .password("ENC_PW")
            .duration(User.activeDuration())
            .createdAt(Instant.now(fixedClock))
            .build();

    ReflectionTestUtils.setField(user, "id", id);
    ReflectionTestUtils.setField(user, "role", UserRole.STUDENT);
    return user;
  }

  private Post buildPost(UUID id, User author) {
    Category category = Category.builder().name("학사").displayOrder(1).build();
    ReflectionTestUtils.setField(category, "id", UUID.randomUUID());

    Affiliation affiliation = Affiliation.builder().name("총학생회").build();
    ReflectionTestUtils.setField(affiliation, "id", UUID.randomUUID());

    Council council =
        Council.builder()
            .affiliation(affiliation)
            .adminUser(author)
            .minVote(10)
            .approveRatioThreshold(new BigDecimal("0.5000"))
            .updatedAt(Instant.now(fixedClock))
            .build();
    ReflectionTestUtils.setField(council, "id", UUID.randomUUID());

    Post post =
        Post.builder()
            .user(author)
            .council(council)
            .category(category)
            .title("제목")
            .content("본문")
            .postVotingDuration(PostVotingDuration.ONE_WEEK)
            .minVoteSnapshot(council.getMinVote())
            .approveRatioSnapshot(council.getApproveRatioThreshold())
            .createdAt(Instant.now(fixedClock))
            .build();
    ReflectionTestUtils.setField(post, "id", id);
    post.updateStatus(PostStatus.VOTING);
    return post;
  }

  private Comment buildComment(UUID id, Post post, User user, Comment rootComment, String content) {
    Comment comment =
        Comment.builder()
            .post(post)
            .user(user)
            .rootComment(rootComment)
            .content(content)
            .createdAt(Instant.now(fixedClock))
            .build();
    ReflectionTestUtils.setField(comment, "id", id);
    return comment;
  }
}
