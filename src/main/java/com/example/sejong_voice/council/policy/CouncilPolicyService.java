package com.example.sejong_voice.council.policy;

import com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse;
import com.example.sejong_voice.council.policy.entity.CouncilPolicy;
import com.example.sejong_voice.council.policy.exception.PolicyErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouncilPolicyService {

  private final CouncilPolicyRepository councilPolicyRepository;

  public CouncilPolicyResponse get(UUID councilId) {
    CouncilPolicy policy = councilPolicyRepository.findById(councilId)
        .orElseThrow(() -> new BusinessException(PolicyErrorCode.POLICY_NOT_FOUND));

    log.info("정책 조회: councilId={}, policy={}", councilId, policy);
    return CouncilPolicyResponse.from(policy);
  }
}