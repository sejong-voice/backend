package com.example.sejong_voice.post;

import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.policy.entity.CouncilPolicy;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.dto.PostDetailResponse;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

  private final PostRepository postRepository;

  @Transactional
  public Post create(
      User user,
      Council council,
      String title,
      String content,
      PostVotingDuration postVotingDuration
  ) {
    CouncilPolicy policy = council.getPolicy();

    Post post = Post.builder()
        .user(user)
        .council(council)
        .title(title)
        .content(content)
        .postVotingDuration(postVotingDuration)
        .minVoteSnapshot(policy.getMinVote())
        .approveRatioSnapshot(policy.getApproveRatioThreshold())
        .build();

    log.info("게시물 생성: userId={}, councilId={}", user.getId(), council.getId());
    return postRepository.save(post);
  }

  @Transactional
  public PostDetailResponse get(UUID id) {
    Post post = postRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

    // 조회수 증가
    post.incrementViewCount();
    log.info("게시물 상세 조회: postId={}", id);

    return PostDetailResponse.from(post);
  }

  public Page<PostListResponse> getList(PostSearchCondition condition, Pageable pageable) {
    Page<PostListResponse> postList = postRepository.search(condition, pageable);

    log.info(
        "게시물 목록 조회: 실제건수={}, 전체건수={}, 검색조건={}",
        postList.getNumberOfElements(),
        postList.getTotalElements(),
        condition
    );

    return postList;
  }

  public Post getById(UUID id) {
    return postRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
  }

  @Transactional
  public PostDetailResponse update(
      UUID postId,
      String title,
      String content,
      UUID currentUserId
  ) {
    // 1. 게시물 조회
    Post post = postRepository.findByIdWithUser(postId)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

    // 2. 권한 체크
    if (!post.getUser().getId().equals(currentUserId)) {
      throw new BusinessException(PostErrorCode.ACCESS_DENIED);
    }

    // 3. 내용 수정
    post.update(title, content);
    log.info("게시물 수정: {}", postId);

    // 4. 변경된 데이터 반환
    return PostDetailResponse.from(post);
  }

  @Transactional
  public void delete(UUID postId) {
    Post post = getById(postId);
    post.delete();
    log.info("게시물 삭제 완료: postId={}", postId);
  }

  @Transactional
  public void evaluateAndClose(UUID postId, long agreeCount, long totalCount) {
    Post post = getById(postId);
    post.evaluateAndClose(agreeCount, totalCount);
    log.info(
        "게시물 투표 종료: postId={}, status={}, result={}",
        postId, post.getStatus(), post.getResultContent()
    );
  }

  @Transactional
  public void updateResult(
      UUID postId,
      PostStatus status,
      String resultContent
  ) {
    Post post = getById(postId);
    post.updateResult(status, resultContent);
    log.info("게시물 결과 업데이트: postId={}, status={}", postId, status);
  }

  public List<Post> findAllByStatusAndVotingEndAtBefore(PostStatus status, LocalDateTime now) {
    return postRepository.findAllByStatusAndVotingEndAtBefore(status, now);
  }
}