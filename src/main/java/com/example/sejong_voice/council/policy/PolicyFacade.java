package com.example.sejong_voice.council.policy;

import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse;
import com.example.sejong_voice.council.policy.exception.PolicyErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PolicyFacade {

  private final PolicyService policyService;
  private final UserService userService;
  private final CouncilService councilService;

  @Transactional(readOnly = true)
  public CouncilPolicyResponse getPolicy(UUID councilId) {
    return policyService.get(councilId);
  }

  @Transactional
  public void updatePolicy(
      UUID councilId, UUID userId, Integer minVote, BigDecimal approveRatioThreshold) {
    // 1. 유저 조회
    User user = userService.getById(userId);

    // 2. 권한 체크 (SUPER 권한이거나 해당 학생회의 운영진인 경우만 가능)
    boolean isSuper = user.getRole() == UserRole.SUPER;
    boolean isMember = councilService.isAdmin(userId, councilId);

    if (!isSuper && !isMember) {
      throw new BusinessException(PolicyErrorCode.ACCESS_DENIED);
    }

    // 3. 수정 위임
    policyService.update(councilId, minVote, approveRatioThreshold);
  }
}
