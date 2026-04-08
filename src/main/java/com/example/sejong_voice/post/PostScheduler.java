package com.example.sejong_voice.post;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostScheduler {

  private final PostFacade postFacade;
  private final PostService postService;
  private final Clock clock;

  /**
   * 매일 자정(00:00)에 종료 시간이 지난 투표 게시물을 찾아 자동으로 종료 처리
   */
  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  public void autoCloseExpiredVotes() {
    log.info("[Scheduler] 만료된 투표 게시물 자동 종료 체크 시작...");

    List<Post> expiredPosts = postService.findClosable(
        PostStatus.VOTING,
        Instant.now(clock)
    );

    if (expiredPosts.isEmpty()) {
      return;
    }

    log.info("[Scheduler] {}개의 만료된 게시물을 발견했습니다.", expiredPosts.size());

    for (Post post : expiredPosts) {
      try {
        postFacade.processClosing(post.getId());
      } catch (Exception e) {
        log.error("[Scheduler] 게시물 자동 종료 처리 중 오류 발생: postId={}", post.getId(), e);
      }
    }

    log.info("[Scheduler] 자동 종료 체크 완료.");
  }
}
