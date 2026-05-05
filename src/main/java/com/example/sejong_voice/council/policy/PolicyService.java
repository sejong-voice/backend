package com.example.sejong_voice.council.policy;

import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

  private final CouncilService councilService;
  private final Clock clock;

  public CouncilPolicyResponse get(UUID councilId) {
    Council council = councilService.getById(councilId);
    log.info("정책 조회: councilId={}", councilId);
    return CouncilPolicyResponse.from(council);
  }

  @Transactional
  public void update(UUID councilId, Integer minVote, BigDecimal approveRatioThreshold) {
    Council council = councilService.getById(councilId);
    council.updatePolicy(minVote, approveRatioThreshold, Instant.now(clock));
    log.info(
        "정책 수정: councilId={}, minVote={}, ratio={}", councilId, minVote, approveRatioThreshold);
  }
}
