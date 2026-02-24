package com.example.sejong_voice.comment;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.comment.dto.CommentCreateRequest;
import com.example.sejong_voice.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<UUID> create(
      @RequestBody @Valid CommentCreateRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    UUID id = commentService.create(
        userPrincipal.getUserId(),
        request.postId(),
        request.parentId(),
        request.content()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(id);
  }

  @GetMapping("/post/{postId}")
  public ResponseEntity<List<CommentResponse>> get(@PathVariable UUID postId) {
    return ResponseEntity.ok(commentService.getByPostId(postId));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID id,
      @AuthUser UserPrincipal userPrincipal
  ) {
    commentService.delete(userPrincipal.getUserId(), id);
    return ResponseEntity.noContent().build();
  }
}
