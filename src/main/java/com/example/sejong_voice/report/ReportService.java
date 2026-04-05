package com.example.sejong_voice.report;


import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import com.example.sejong_voice.comment.repository.CommentRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostModerationStatus;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.report.entity.CommentReport;
import com.example.sejong_voice.report.entity.CommentReportReason;
import com.example.sejong_voice.report.entity.PostReport;
import com.example.sejong_voice.report.entity.PostReportReason;
import com.example.sejong_voice.report.exception.ReportErrorCode;
import com.example.sejong_voice.report.repository.CommentReportRepository;
import com.example.sejong_voice.report.repository.PostReportRepository;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.history.entity.PostStatusHistory;
import com.example.sejong_voice.history.repository.PostStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final PostReportRepository postReportRepository;
    private final PostStatusHistoryRepository postStatusHistoryRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    //게시글 신고
    @Transactional
    public UUID reportPost(
            UUID reporterId,//신고하는 사람 id
            UUID postId,
            PostReportReason reason,
            String description
    ) {
        //신고하는 유저가 있는지 먼저 찾음
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        //신고할 target_Post가 있는지 찾음
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.POST_NOT_FOUND));

        //자기가 작성한 게시글에 대해서 신고는 안됨
        if (post.getUser().getId().equals(reporterId)) {
            throw new BusinessException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        //동일 게시글에 대해 중복 신고가 안됨
        if (postReportRepository.existsByPostIdAndReporterUserId(postId, reporterId)) {
            throw new BusinessException(ReportErrorCode.DUPLICATE_POST_REPORT);
        }

        try {
            PostReport saved = postReportRepository.save(
                    PostReport.builder()
                            .post(post)
                            .reporterUser(reporter)
                            .reason(reason)
                            .description(description)
                            .createdAt(Instant.now(clock))
                            .build()
            );
            //PostReport의 id 반환
            return saved.getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ReportErrorCode.DUPLICATE_POST_REPORT);
        }
    }


    //댓글 신고
    @Transactional
    public UUID reportComment(
            UUID reporterId,//신고하는 사람 id
            UUID commentId,
            CommentReportReason reason
    ) {
        //신고하는 사람이 실존하는지
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        //신고할 댓글이 실존하는지
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.COMMENT_NOT_FOUND));

        //댓글의 상태가 deleted라면 신고 못함 -> 신고할 대상이 없다는 예외처리
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new BusinessException(ReportErrorCode.COMMENT_NOT_FOUND);
        }

        //댓글 작성자 == 신고하는 사람 : 자기가 작성한 댓글은 신고 못함
        if (comment.getUser().getId().equals(reporterId)) {
            throw new BusinessException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        //댓글 중복 신고 방지
        if (commentReportRepository.existsByCommentIdAndUserId(commentId, reporterId)) {
            throw new BusinessException(ReportErrorCode.DUPLICATE_COMMENT_REPORT);
        }

        try {
            CommentReport saved = commentReportRepository.save(
                    CommentReport.builder()
                            .comment(comment)
                            .user(reporter)
                            .reason(reason)
                            .createdAt(Instant.now(clock))
                            .build()
            );
            return saved.getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ReportErrorCode.DUPLICATE_COMMENT_REPORT);
        }
    }



    // 게시글 제재
    @Transactional
    public void moderatePost(
            UUID adminUserId ,
            UUID postId,
            PostModerationStatus actionType
    ) {
        //제재할 post. 없으면 예외처리
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.POST_NOT_FOUND));

        // 변경 전 상태 임시 저장
        PostModerationStatus fromStatus = post.getVisibilityStatus();


        switch (actionType) {
            case ACTIVE -> post.activate();   // ACTIVE 복구
            case DELETED -> post.delete(Instant.now(clock));    // 삭제 처리
            case BLINDED -> post.blind();     // 블라인드 처리
        }

        // 히스토리 저장 (builder 사용)
        postStatusHistoryRepository.save(
                PostStatusHistory.builder()
                        .userId(adminUserId)
                        .post(post)
                        .fromStatus(fromStatus)
                        .toStatus(actionType)
                        .createdAt(Instant.now(clock))
                        .build()
        );
    }

    // 댓글 제재
    @Transactional
    public void moderateComment(UUID commentId, CommentStatus actionType) {
        //제제할 comment. 없음 예외처리.
        //참고로 권한은 securityConfig에서 체크하기 때문에 굳이...? api만 securityConfig에 권한체크해서 등록해두면 됨
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.COMMENT_NOT_FOUND));

        switch (actionType) {
            case ACTIVE -> comment.activate();  // ACTIVE 복구
            case DELETED -> comment.delete(Instant.now(clock));   // 삭제 처리
            case BLINDED -> comment.blind();    // 블라인드 처리
        }
    }


    // 신고 내역 조회 (User별, Post별, ) -> 다른 방식으로 조회하는거 필요하면 추가, 자동화할라면 메서드 몇 개 만들어야할껄

    //User별 신고 내역 조회 : 유저A가 몇 번 신고받은건지
    public int getReceivedCommentReportCount(UUID targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        long count = commentReportRepository.countByComment_User_Id(targetUserId);
        //commentReport(신고받은 댓글) -> comment(댓글) -> user(댓글 작성자) -> user_id(studentNo로 해도 되긴함. 상관 없음)
        return Math.toIntExact(count); // int로 반환하고 싶다면
    }

    //Post별 신고 내역 조회 : 게시글A가 몇 번 신고받은건지
    public long getReceivedPostReportCount(UUID postId) {
        if (postRepository.findByIdAndDeletedAtIsNull(postId).isEmpty()) {
            throw new BusinessException(ReportErrorCode.POST_NOT_FOUND);
            //개인적 질문 -> ReportErrorCode, PostErrorCode랑 겹치는 부분이 있는데 어디를 메인으로 정리해줘야함??
        }

        long count = postReportRepository.countByPost_Id(postId);
        // PostReport테이블에서 Post_id를 기준으로 count
        return Math.toIntExact(count);
    }

}



// 검증 요소
//중복 신고 금지 -> 엔티티에서 쿼리로 처리해둠
//자기 글/댓글 신고 금지 => 굳이? 누가 자기꺼 신고하냐
//대상 존재 여부 -> 레포에서 먼저 찾고 없음 예외 던지는식
//관리자 권한 확인(제재/조회) -> api를 securityConfig에서 권한 제한 걸어두면 됨
