package com.example.sejong_voice.policy;

import com.example.sejong_voice.policy.dto.CouncilPolicyResponse;
import com.example.sejong_voice.policy.entity.CouncilPolicy;
import com.example.sejong_voice.policy.exception.PolicyErrorCode;
import com.example.sejong_voice.policy.repository.PolicyRepository;
import com.example.sejong_voice.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

  private final PolicyRepository councilPolicyRepository;

  public CouncilPolicyResponse get(UUID councilId) {
    CouncilPolicy policy = getById(councilId);
    log.info("정책 조회: councilId={}, policy={}", councilId, policy);
    return CouncilPolicyResponse.from(policy);
  }

  public CouncilPolicy getById(UUID councilId) {
    return councilPolicyRepository.findById(councilId)
        .orElseThrow(() -> new BusinessException(PolicyErrorCode.POLICY_NOT_FOUND));
  }

  @Transactional
  public void update(
      UUID councilId,
      Integer minVote,
      BigDecimal approveRatioThreshold
  ) {
    CouncilPolicy policy = getById(councilId);
    policy.update(minVote, approveRatioThreshold);
    log.info("정책 수정: councilId={}, minVote={}, ratio={}", councilId, minVote, approveRatioThreshold);
  }
}