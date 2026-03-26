package com.example.sejong_voice.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {

  private final ImageFacade imageFacade;

  @Scheduled(cron = "0 0 * * * *")
  public void cleanupExpiredTempImages() {
    log.info("[Scheduler] 만료된 TEMP 이미지 정리 시작");
    imageFacade.cleanupExpiredTempImages();
    log.info("[Scheduler] 만료된 TEMP 이미지 정리 완료");
  }
}
