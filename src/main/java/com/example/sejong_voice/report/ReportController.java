package com.example.sejong_voice.report;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.report.dto.CommentModerationRequest;
import com.example.sejong_voice.report.dto.CommentReportRequest;
import com.example.sejong_voice.report.dto.PostModerationRequest;
import com.example.sejong_voice.report.dto.PostReportRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  // 게시글 신고
  @PostMapping("/reports/posts/{postId}")
  public ResponseEntity<UUID> reportPost(
      @PathVariable UUID postId,
      @RequestBody @Valid PostReportRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    UUID reportId = reportService.reportPost(
        userPrincipal.getUserId(),
        postId,
        request.reason(),
        request.description()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(reportId);
  }

  // 댓글 신고
  @PostMapping("/reports/comments/{commentId}")
  public ResponseEntity<UUID> reportComment(
      @PathVariable UUID commentId,
      @RequestBody @Valid CommentReportRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    UUID reportId = reportService.reportComment(
        userPrincipal.getUserId(),//신고자 id
        commentId,
        request.reason()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(reportId);
  }

  // 게시글 제재
  @PatchMapping("/admin/moderations/posts/{postId}")
  public ResponseEntity<Void> moderatePost(
      @PathVariable UUID postId,
      @RequestBody @Valid PostModerationRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    reportService.moderatePost(
            userPrincipal.getUserId(), // principal에서 userId 추출
            postId,
            request.actionType()
    );
    return ResponseEntity.ok().build();
  }

  // 댓글 제재
  @PatchMapping("/admin/moderations/comments/{commentId}")
  public ResponseEntity<Void> moderateComment(
      @PathVariable UUID commentId,
      @RequestBody @Valid CommentModerationRequest request
  ) {
    reportService.moderateComment(commentId, request.actionType());
    return ResponseEntity.ok().build();
  }

  // User별 댓글 신고 수 조회
  @GetMapping("/admin/reports/users/{userId}/comments/count")//프론트에서 userid를 알 수 있나? 학번도 아닌데
  public ResponseEntity<Integer> getReceivedCommentReportCount(@PathVariable UUID userId) {
    return ResponseEntity.ok(reportService.getReceivedCommentReportCount(userId));
  }

  // Post별 신고 수 조회
  @GetMapping("/admin/reports/posts/{postId}/count") // 프론트에서 postid를 어케알아? 어떻게 조회할 방법도 없는데
  public ResponseEntity<Long> getReceivedPostReportCount(@PathVariable UUID postId) {
    return ResponseEntity.ok(reportService.getReceivedPostReportCount(postId));
  }
}
