package com.example.sejong_voice.post.vote;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.post.vote.dto.VoteRequest;
import com.example.sejong_voice.post.vote.dto.VoteSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts/{postId}/votes")
@RequiredArgsConstructor
public class PostVoteController {

  private final PostVoteService postVoteService;

  @PutMapping
  public ResponseEntity<Void> cast(
      @PathVariable UUID postId,
      @RequestBody @Valid VoteRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    postVoteService.cast(postId, userPrincipal.getUserId(), request.choice());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/summary")
  public ResponseEntity<VoteSummaryResponse> getSummary(
      @PathVariable UUID postId
  ) {
    return ResponseEntity.ok(postVoteService.getSummary(postId));
  }
}