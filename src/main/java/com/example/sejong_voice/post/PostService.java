package com.example.sejong_voice.post;

import com.example.sejong_voice.category.entity.Category;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

  private final PostRepository postRepository;
  private final Clock clock;

  @Transactional
  public Post create(
      User user,
      Council council,
      Category category,
      String title,
      String content,
      PostVotingDuration postVotingDuration
  ) {
    CouncilPolicy policy = council.getPolicy();

    Post post = Post.builder()
        .user(user)
        .council(council)
        .category(category)
        .title(title)
        .content(content)
        .postVotingDuration(postVotingDuration)
        .minVoteSnapshot(policy.getMinVote())
        .approveRatioSnapshot(policy.getApproveRatioThreshold())
        .createdAt(Instant.now(clock))
        .build();

    log.info(
        "게시물 생성: userId={}, councilId={}, categoryId={}",
        user.getId(),
        council.getId(),
        category.getId()
    );
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

  public Page<PostListResponse> getPosts(PostSearchCondition condition, Pageable pageable) {
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

  public Post getByIdForUpdate(UUID id) {
    return postRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
  }

  @Transactional
  public void delete(UUID postId) {
    Post post = getById(postId);
    post.delete(Instant.now(clock));
    log.info("게시물 삭제 완료: postId={}", postId);
  }

  @Transactional
  public void evaluateAndClose(UUID postId, long agreeCount, long totalCount) {
    Post post = getById(postId);
    post.evaluateAndClose(agreeCount, totalCount, Instant.now(clock));
    log.info(
        "게시물 투표 종료: postId={}, status={}",
        postId, post.getStatus()
    );
  }

  public List<Post> findClosable(PostStatus status, Instant now) {
    return postRepository.findAllByStatusAndVotingEndAtBefore(status, now);
  }
}
