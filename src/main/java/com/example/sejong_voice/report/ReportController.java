package com.example.sejong_voice.report;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.report.dto.CommentModerationRequest;
import com.example.sejong_voice.report.dto.CommentReportRequest;
import com.example.sejong_voice.report.dto.PostModerationRequest;
import com.example.sejong_voice.report.dto.PostReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "신고/제재 API", description = "게시글/댓글 신고 및 관리자 제재 처리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  // 게시글 신고
  @Operation(
      summary = "게시글 신고",
      description = "사용자가 특정 게시글을 신고합니다.SPAM(스팸성 게시글),ABUSE(오남용),HATE(그냥 싫음),PRIVACY(프라이버시 침범), DUPLICATE(중복 게시글), OTHER(기타 등등)"
  )
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
  @Operation(
      summary = "댓글 신고",
      description = "사용자가 특정 댓글을 신고합니다.SPAM(스팸성 댓글),ABUSE(오남용),HATE(그냥 싫음),PRIVACY(프라이버시 침범), OTHER(기타 등등)"
  )
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
  @Operation(
      summary = "게시글 제재 처리",
      description = "관리자가 신고된 게시글에 제재 액션을 적용합니다. ACTIVE -> DELETED 혹은 BLINDED로 전환"
  )
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
  @Operation(
      summary = "댓글 제재 처리",
      description = "관리자가 신고된 댓글에 제재 액션을 적용합니다. ACTIVE -> DELETED 혹은 BLINDED로 전환"
  )
  @PatchMapping("/admin/moderations/comments/{commentId}")
  public ResponseEntity<Void> moderateComment(
      @PathVariable UUID commentId,
      @RequestBody @Valid CommentModerationRequest request
  ) {
    reportService.moderateComment(commentId, request.actionType());
    return ResponseEntity.ok().build();
  }

  // User별 댓글 신고 수 조회
  @Operation(
      summary = "사용자 댓글 신고 수 조회",
      description = "특정 사용자가 받은 댓글 신고 건수를 조회합니다. 관리자가 해당 결과를 참고하여 제재를 할건지 결정"
  )
  @GetMapping("/admin/reports/users/{userId}/comments/count")//프론트에서 userid를 알 수 있나? 학번도 아닌데
  public ResponseEntity<Integer> getReceivedCommentReportCount(@PathVariable UUID userId) {
    return ResponseEntity.ok(reportService.getReceivedCommentReportCount(userId));
  }

  // Post별 신고 수 조회
  @Operation(
      summary = "게시글 신고 수 조회",
      description = "특정 게시글이 받은 신고 건수를 조회합니다. 관리자가 해당 결과를 참고하여 제재를 할건지 결정"
  )
  @GetMapping("/admin/reports/posts/{postId}/count") // 프론트에서 postid를 어케알아? 어떻게 조회할 방법도 없는데
  public ResponseEntity<Long> getReceivedPostReportCount(@PathVariable UUID postId) {
    return ResponseEntity.ok(reportService.getReceivedPostReportCount(postId));
  }
}
