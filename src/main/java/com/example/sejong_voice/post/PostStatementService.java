package com.example.sejong_voice.post;

import com.example.sejong_voice.post.dto.PostStatementView;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.post.repository.PostStatementRepository;
import com.example.sejong_voice.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostStatementService {

  private final PostStatementRepository postStatementRepository;
  private final Clock clock;

  /**
   * 게시글에 새 입장문을 생성합니다.
   *
   * @param post      대상 게시글
   * @param createdBy 입장문 작성자
   * @param sequence  입장문 순번
   * @param content   입장문 내용
   * @return 생성된 입장문
   */
  @Transactional
  public PostStatement create(Post post, User createdBy, int sequence, String content) {
    PostStatement postStatement = postStatementRepository.save(PostStatement.builder()
        .post(post)
        .createdBy(createdBy)
        .sequence(sequence)
        .content(content)
        .createdAt(Instant.now(clock))
        .build());

    log.info("입장문 생성: postId={}, statementId={}, sequence={}", post.getId(), postStatement.getId(),
        postStatement.getSequence());
    return postStatement;
  }

  /**
   * 게시글의 입장문 개수를 조회합니다.
   *
   * @param postId 게시글 ID
   * @return 입장문 개수
   */
  public long countStatements(UUID postId) {
    return postStatementRepository.countByPostId(postId);
  }

  /**
   * 게시글의 입장문 목록을 순서대로 조회합니다.
   *
   * @param postId 게시글 ID
   * @return 입장문 목록
   */
  public List<PostStatementView> getStatements(UUID postId) {
    List<PostStatement> postStatements = postStatementRepository.findAllByPostIdOrderBySequenceAsc(postId);
    return PostStatementView.from(postStatements);
  }
}
