package com.example.sejong_voice.post;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.post.dto.PostCreateRequest;
import com.example.sejong_voice.post.dto.PostDetailResponse;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostResultRequest;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.dto.PostUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "게시글 API", description = "게시글 생성/조회/수정/삭제 API (VOTING(투표중), APPROVED(승인됨), PENDING(투표 미달로 기각), COMPLETED(승인된 투표에 대한 응답완료), REJECTED(모종의 이유로 승인은 되었으나 답변은 하지 못하는 경우 거절)))")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostFacade postFacade;

  @Operation(
          summary = "게시글 생성",
          description = "새 게시글(민원)을 생성합니다."
  )
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

  @Operation(
      summary = "게시글 목록 조회",
      description = "조건/페이징 기반으로 게시글 목록을 조회합니다.ACTIVE(보여지는 게시글),DELETED(삭제할 게시글),BLINDED(가림 처리한 게시글). ACTIVE 상태만 필터링해서 프론트로 보내줄거임"
  )
  @GetMapping
  public ResponseEntity<Page<PostListResponse>> getList(
      @ParameterObject @ModelAttribute PostSearchCondition condition,
      @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
      @AuthUser UserPrincipal userPrincipal
  ) {
    UUID currentUserId = (userPrincipal != null) ? userPrincipal.getUserId() : null;
    PostSearchCondition postSearchCondition = condition.withCurrentUserId(currentUserId);

    return ResponseEntity.ok(postFacade.getList(postSearchCondition, pageable));
  }

  @Operation(
          summary = "게시글 상세 조회",
          description = "게시글 ID로 단건 상세 정보를 조회합니다. ACTIVE(보여지는 게시글),DELETED(삭제할 게시글),BLINDED(가림 처리한 게시글). ACTIVE 상태만 필터링해서 프론트로 보내줄거임"
  )
  @GetMapping("/{postId}")
  public ResponseEntity<PostDetailResponse> get(@PathVariable UUID postId) {
    return ResponseEntity.ok(postFacade.getPost(postId));
  }

  @Operation(
      summary = "게시글 수정",
      description = "작성자 권한으로 게시글 제목/내용을 수정합니다."
  )
  @PutMapping("/{postId}")
  public ResponseEntity<PostDetailResponse> update(
      @PathVariable UUID postId,
      @RequestBody @Valid PostUpdateRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    PostDetailResponse response = postFacade.update(
        postId,
        request.title(),
        request.content(),
        userPrincipal.getUserId()
    );
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "게시글 삭제",
      description = "작성자 권한으로 게시글을 삭제합니다."
  )
  @DeleteMapping("/{postId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID postId,
      @AuthUser UserPrincipal userPrincipal
  ) {
    postFacade.delete(postId, userPrincipal.getUserId());
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "게시글 조기 종료",
      description = "학생회가 현재 진행 중인 게시글 투표를 조기 종료합니다. 조건은 minVote(최소투표수), Ratio(투표비율)를 만족한 경우에 가능합니다"
  )
  @PostMapping("/{postId}/close")
  public ResponseEntity<Void> closeEarly(
      @PathVariable UUID postId,
      @AuthUser UserPrincipal userPrincipal
  ) {
    postFacade.closeEarly(postId, userPrincipal.getUserId());
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "게시글 처리 결과(입장문) 등록",
      description = "게시글 상태/결과 내용을 등록 또는 수정합니다."
  )
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
