package com.example.sejong_voice.post;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.post.dto.PostCreateRequest;
import com.example.sejong_voice.post.dto.PostResponse;
import com.example.sejong_voice.post.dto.PostResultRequest;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.dto.PostUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostFacade postFacade;

  @PostMapping
  public ResponseEntity<UUID> create(
      @RequestBody @Valid PostCreateRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    UUID id = postFacade.create(
        userPrincipal.getUserId(),
        request.title(),
        request.content(),
        request.councilId(),
        request.postVotingDuration()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(id);
  }

  @GetMapping("/{postId}")
  public ResponseEntity<PostResponse> get(@PathVariable UUID postId) {
    return ResponseEntity.ok(postFacade.getPost(postId));
  }

  @GetMapping
  public ResponseEntity<Page<PostResponse>> getAll(
      @ParameterObject @ModelAttribute PostSearchCondition condition,
      @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
      @AuthUser UserPrincipal userPrincipal
  ) {
    UUID currentUserId = (userPrincipal != null) ? userPrincipal.getUserId() : null;
    PostSearchCondition postSearchCondition = condition.withCurrentUserId(currentUserId);

    return ResponseEntity.ok(postFacade.getAllPosts(postSearchCondition, pageable));
  }

  @PutMapping("/{postId}")
  public ResponseEntity<PostResponse> update(
      @PathVariable UUID postId,
      @RequestBody @Valid PostUpdateRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    PostResponse response = postFacade.update(
        postId,
        request.title(),
        request.content(),
        userPrincipal.getUserId()
    );
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{postId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID postId,
      @AuthUser UserPrincipal userPrincipal
  ) {
    postFacade.delete(postId, userPrincipal.getUserId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{postId}/close")
  public ResponseEntity<Void> closeEarly(
      @PathVariable UUID postId,
      @AuthUser UserPrincipal userPrincipal
  ) {
    postFacade.closeEarly(postId, userPrincipal.getUserId());
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{postId}/result")
  public ResponseEntity<Void> updateResult(
      @PathVariable UUID postId,
      @RequestBody @Valid PostResultRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    postFacade.updateResult(
        postId,
        userPrincipal.getUserId(),
        request.status(),
        request.resultContent()
    );
    return ResponseEntity.ok().build();
  }
}
