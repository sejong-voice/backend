package com.example.sejong_voice.post;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.post.dto.PostCreateRequest;
import com.example.sejong_voice.post.dto.PostResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;

  @PostMapping
  public ResponseEntity<UUID> create(
      @RequestBody @Valid PostCreateRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    UUID id = postService.create(
        userPrincipal.getUserId(),
        request.title(),
        request.content(),
        request.councilId(),
        request.postVotingDuration()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(id);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PostResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(postService.get(id));
  }

  @GetMapping
  public ResponseEntity<Page<PostResponse>> getAll(
      @ParameterObject @ModelAttribute PostSearchCondition condition,
      @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
      @AuthUser UserPrincipal userPrincipal
  ) {
    PostSearchCondition postSearchCondition = condition.withCurrentUserId(userPrincipal.getUserId());

    return ResponseEntity.ok(postService.getAll(postSearchCondition, pageable));
  }

  @PutMapping("/{postId}")
  public ResponseEntity<PostResponse> update(
      @PathVariable UUID postId,
      @RequestBody @Valid PostUpdateRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    PostResponse response = postService.update(
        postId,
        request.title(),
        request.content(),
        userPrincipal.getUserId()
    );
    return ResponseEntity.ok(response);
  }
}