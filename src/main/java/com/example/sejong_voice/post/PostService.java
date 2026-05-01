package com.example.sejong_voice.post;

import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.entity.User;
import java.math.BigDecimal;
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
    Post post = Post.builder()
        .user(user)
        .council(council)
        .category(category)
        .title(title)
        .content(content)
        .postVotingDuration(postVotingDuration)
        .minVoteSnapshot(council.getMinVote())
        .approveRatioSnapshot(council.getApproveRatioThreshold())
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

  public Post get(UUID id) {
    return postRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
  }

  public Post getByIdForUpdate(UUID id) {
    return postRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
  }

  @Transactional
  public void delete(UUID postId) {
    Post post = get(postId);
    post.delete(Instant.now(clock));
    log.info("게시물 삭제 완료: postId={}", postId);
  }

  @Transactional
  public void close(UUID postId, long agreeCount, long totalCount) {
    Post post = get(postId);
    if (post.getStatus() != PostStatus.VOTING) {
      return;
    }

    post.close(Instant.now(clock));
    if (meetsApprovalCriteria(post, agreeCount, totalCount)) {
      post.updateStatus(PostStatus.APPROVED);
    } else {
      post.updateStatus(PostStatus.PENDING);
    }

    log.info(
        "게시물 투표 종료: postId={}, status={}",
        postId, post.getStatus()
    );
  }

  @Transactional
  public void closeEarly(UUID postId, long agreeCount, long totalCount) {
    Post post = get(postId);

    if (!canCloseEarly(post, agreeCount, totalCount)) {
      throw new BusinessException(PostErrorCode.EARLY_CLOSE_NOT_AVAILABLE);
    }

    post.close(Instant.now(clock));
    post.updateStatus(PostStatus.APPROVED);
    log.info("게시물 조기 종료 승인: postId={}, status={}", postId, post.getStatus());
  }

  public boolean canCloseEarly(Post post, long agreeCount, long totalCount) {
    return meetsApprovalCriteria(post, agreeCount, totalCount);
  }

  private boolean meetsApprovalCriteria(Post post, long agreeCount, long totalCount) {
    if (post.getStatus() != PostStatus.VOTING) {
      return false;
    }

    if (totalCount < post.getMinVoteSnapshot()) {
      return false;
    }

    if (totalCount <= 0) {
      return false;
    }

    BigDecimal requiredAgreeCount = post.getApproveRatioSnapshot()
        .multiply(BigDecimal.valueOf(totalCount));

    return BigDecimal.valueOf(agreeCount).compareTo(requiredAgreeCount) >= 0;
  }

  public List<Post> findClosable(PostStatus status, Instant now) {
    return postRepository.findAllByStatusAndVotingEndAtBefore(status, now);
  }
}
