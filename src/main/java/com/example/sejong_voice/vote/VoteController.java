package com.example.sejong_voice.vote;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.vote.dto.VoteDetailResponse;
import com.example.sejong_voice.vote.dto.VoteRequest;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "투표 API", description = "게시글 투표 및 집계 조회 API")
@RestController
@RequestMapping("/api/v1/posts/{postId}/votes")
@RequiredArgsConstructor
public class VoteController {

  private final VoteFacade voteFacade;

  @Operation(
      summary = "투표하기",
      description = "현재 로그인한 사용자가 게시글에 찬성/반대 투표를 등록 또는 변경합니다."
  )
  @PutMapping
  public ResponseEntity<Void> cast(
      @PathVariable UUID postId,
      @RequestBody @Valid VoteRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    voteFacade.cast(postId, userPrincipal.getUserId(), request.choice());
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "투표 집계 조회",
      description = "특정 게시글의 현재 투표 집계 정보를 조회합니다."
  )
  @GetMapping("/summary")
  public ResponseEntity<VoteSummaryResponse> getSummary(
      @PathVariable UUID postId
  ) {
    return ResponseEntity.ok(voteFacade.getSummary(postId));
  }

  @Operation(
      summary = "투표 상세 결과 조회",
      description = "학생회 계정 또는 SUPER 권한 사용자가 특정 게시글의 투표 상세 결과와 투표자 목록을 조회합니다."
  )
  @GetMapping("/details")
  public ResponseEntity<VoteDetailResponse> getDetails(
      @PathVariable UUID postId,
      @AuthUser UserPrincipal userPrincipal
  ) {
    return ResponseEntity.ok(voteFacade.getDetails(postId, userPrincipal.getUserId()));
  }
}
