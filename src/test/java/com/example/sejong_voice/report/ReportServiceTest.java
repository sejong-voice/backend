package com.example.sejong_voice.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.comment.repository.CommentRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostModerationStatus;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.report.entity.CommentReportReason;
import com.example.sejong_voice.report.entity.PostReport;
import com.example.sejong_voice.report.entity.PostReportReason;
import com.example.sejong_voice.report.exception.ReportErrorCode;
import com.example.sejong_voice.report.repository.CommentReportRepository;
import com.example.sejong_voice.report.repository.PostReportRepository;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock private PostReportRepository postReportRepository;
    @Mock private CommentReportRepository commentReportRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("게시글 신고 성공: 정상 저장 후 신고 ID 반환")
    void reportPost_success() {
        // given: 신고자/대상 게시글이 존재하고, 자기신고/중복신고가 아닌 상태를 준비
        UUID reporterId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        User reporter = mock(User.class);
        User author = mock(User.class);
        Post post = mock(Post.class);
        PostReport saved = mock(PostReport.class);

        //사전 상태(신고자/대상 게시글/작성자 관계)
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));
        when(post.getUser()).thenReturn(author);
        when(author.getId()).thenReturn(UUID.randomUUID()); // 신고자와 다른 작성자

        // 신고 만들기
        when(postReportRepository.existsByPostIdAndReporterUserId(postId, reporterId)).thenReturn(false);//중복 아님
        when(postReportRepository.save(any(PostReport.class))).thenReturn(saved); // 신고 저장
        when(saved.getId()).thenReturn(reportId);

        // when: 게시글 신고 서비스를 실행
        UUID result = reportService.reportPost(reporterId, postId, PostReportReason.SPAM, "광고성");

        // then: 저장된 신고 ID가 반환되고, 저장 로직이 1회 호출됨
        assertThat(result).isEqualTo(reportId); //신고한 게시글이 잘 저장된건지 반환값과 동등비교
        verify(postReportRepository).save(any(PostReport.class));
    }

    @Test
    @DisplayName("게시글 신고 실패: 중복 신고")
    void reportPost_fail_duplicate() {
        // given: 이미 같은 유저가 같은 게시글을 신고한 상태를 준비
        UUID reporterId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        User reporter = mock(User.class);
        User author = mock(User.class);
        Post post = mock(Post.class);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));
        when(post.getUser()).thenReturn(author);
        when(author.getId()).thenReturn(UUID.randomUUID());
        when(postReportRepository.existsByPostIdAndReporterUserId(postId, reporterId)).thenReturn(true);

        // when & then: 서비스 실행 시 중복 신고 예외가 발생해야 함
        assertThatThrownBy(() -> reportService.reportPost(reporterId, postId, PostReportReason.ABUSE, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReportErrorCode.DUPLICATE_POST_REPORT);
    }

    // 게시글 삭제된 게시글 신고 실패

    //댓글 중복 신고 실패

    @Test
    @DisplayName("댓글 신고 실패: 삭제된 댓글")
    void reportComment_fail_deletedComment() {
        // given: 신고 대상 댓글이 DELETED 상태인 상황을 준비
        UUID reporterId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        User reporter = mock(User.class);
        Comment comment = mock(Comment.class);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(comment.getStatus()).thenReturn(CommentStatus.DELETED);

        // when & then: 신고 시 COMMENT_NOT_FOUND 예외가 발생해야 함
        assertThatThrownBy(() -> reportService.reportComment(reporterId, commentId, CommentReportReason.SPAM))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReportErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 제재 성공: BLINDED 처리")
    void moderatePost_blind_success() {
        // given: 제재 대상 게시글이 존재하는 상황을 준비
        UUID postId = UUID.randomUUID();
        Post post = mock(Post.class);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // when: BLINDED 제재를 실행
        reportService.moderatePost(postId, PostModerationStatus.BLINDED);

        // then: 게시글 blind() 메서드가 호출되어야 함
        verify(post).blind();
    }

    //comment 블라인드 처리도 해줘야함

    @Test
    @DisplayName("유저별 댓글 신고 수 조회 성공")
    void getReceivedCommentReportCount_success() {
        // given: 대상 유저가 존재하고 댓글 신고 집계가 5건인 상황을 준비
        UUID targetUserId = UUID.randomUUID();
        when(userRepository.existsById(targetUserId)).thenReturn(true);
        when(commentReportRepository.countByComment_User_Id(targetUserId)).thenReturn(5L);

        // when: 신고 수 조회 실행
        int result = reportService.getReceivedCommentReportCount(targetUserId);

        // then: 기대한 신고 수가 반환되어야 함
        assertThat(result).isEqualTo(5);
    }

    //게시글별 신고 수 조회
}
