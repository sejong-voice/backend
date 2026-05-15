package com.example.sejong_voice.post;

import com.example.sejong_voice.post.repository.PostRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostViewCountService {

  private final PostRepository postRepository;
  private final Map<UUID, LongAdder> pendingViewCounts = new ConcurrentHashMap<>();

  public void recordView(UUID postId) {
    pendingViewCounts.computeIfAbsent(postId, ignored -> new LongAdder()).increment();
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void flushPendingViewCounts() {
    if (pendingViewCounts.isEmpty()) {
      return;
    }

    pendingViewCounts.forEach(
        (postId, counter) -> {
          long count = counter.sumThenReset();
          if (count <= 0) {
            return;
          }

          postRepository.incrementViewCount(postId, count);
          log.debug("게시글 조회수 반영: postId={}, increment={}", postId, count);
        });

    pendingViewCounts.entrySet().removeIf(entry -> entry.getValue().sum() == 0);
  }
}
