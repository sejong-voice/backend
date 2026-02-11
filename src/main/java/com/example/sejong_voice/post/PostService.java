package com.example.sejong_voice.post;

import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.council.policy.entity.CouncilPolicy;
import com.example.sejong_voice.council.repository.CouncilRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.dto.PostResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostVotingDuration;
import com.example.sejong_voice.post.exception.PostErrorCode;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.UserRepository;
import com.example.sejong_voice.user.entity.User;
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
  private final UserRepository userRepository;
  private final CouncilRepository councilRepository;

  @Transactional
  public UUID create(
      UUID userId,
      String title,
      String content,
      UUID councilId,
      PostVotingDuration postVotingDuration
  ) {
    // 1. 연관 엔티티 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    Council council = councilRepository.findById(councilId)
        .orElseThrow(() -> new BusinessException(CouncilErrorCode.COUNCIL_NOT_FOUND));

    // 2. 정책 스냅샷 가져오기 (해당 학생회의 현재 정책)
    CouncilPolicy policy = council.getPolicy();

    // 3. 엔티티 생성 (객체와 스냅샷 전달)
    Post post = Post.builder()
        .user(user)
        .council(council)
        .title(title)
        .content(content)
        .postVotingDuration(postVotingDuration)
        .minVoteSnapshot(policy.getMinVote())
        .approveRatioSnapshot(policy.getApproveRatioThreshold())
        .build();

    UUID id = postRepository.save(post).getId();
    log.info("게시물 생성: postId={}, userId={}, councilId={}", id, userId, councilId);

    return id;
  }

  @Transactional
  public PostResponse get(UUID id) {
    Post post = postRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

    // 조회수 증가
    post.incrementViewCount();
    log.info("게시물 상세 조회: postId={}", id);

    return PostResponse.from(post);
  }

  public Page<PostResponse> getAll(PostSearchCondition condition, Pageable pageable) {
    Page<PostResponse> postList = postRepository.search(condition, pageable);

    log.info(
        "게시물 목록 조회: 실제건수={}, 전체건수={}, 검색조건={}",
        postList.getNumberOfElements(),
        postList.getTotalElements(),
        condition
    );

    return postList;
  }

  @Transactional
  public PostResponse update(
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
    return PostResponse.from(post);
  }
}