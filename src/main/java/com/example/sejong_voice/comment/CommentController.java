package com.example.sejong_voice.comment;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.comment.dto.CommentCreateRequest;
import com.example.sejong_voice.comment.dto.CommentPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글 API", description = "댓글 생성/조회/삭제 API")
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentFacade commentFacade;

  @Operation(summary = "댓글 생성", description = "게시글에 댓글 또는 대댓글을 생성합니다.")
  @PostMapping
  public ResponseEntity<UUID> create(
      @RequestBody @Valid CommentCreateRequest request, @AuthUser UserPrincipal userPrincipal) {
    UUID id =
        commentFacade.create(
            userPrincipal.getUserId(),
            request.postId(),
            request.rootCommentId(),
            request.content());
    return ResponseEntity.status(HttpStatus.CREATED).body(id);
  }

  @Operation(
      summary = "게시글 댓글 목록 조회",
      description = "특정 게시글의 댓글 목록을 조회합니다. ACTIVE(활성 댓글),DELETED(삭제 댓글),BLINDED(가린 댓글)")
  @GetMapping("/post/{postId}")
  public ResponseEntity<CommentPageResponse> getAllByPostId(
      @PathVariable UUID postId,
      @ParameterObject
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
          Pageable pageable,
      @AuthUser UserPrincipal userPrincipal) {
    UUID currentUserId = userPrincipal != null ? userPrincipal.getUserId() : null;
    return ResponseEntity.ok(commentFacade.getAllByPostId(postId, currentUserId, pageable));
  }

  @Operation(summary = "댓글 삭제", description = "작성자 권한으로 댓글을 삭제합니다.")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthUser UserPrincipal userPrincipal) {
    commentFacade.delete(userPrincipal.getUserId(), id);
    return ResponseEntity.noContent().build();
  }
}
