package com.example.sejong_voice.council.policy;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse;
import com.example.sejong_voice.council.policy.exception.PolicyErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyFacadeTest {

  @Mock private PolicyService policyService;
  @Mock private UserService userService;
  @Mock private CouncilService councilService;

  private PolicyFacade policyFacade;

  @BeforeEach
  void setUp() {
    policyFacade = new PolicyFacade(policyService, userService, councilService);
  }

  @Test
  void getPolicy_서비스결과를_반환한다() {
    UUID councilId = UUID.randomUUID();
    CouncilPolicyResponse response = new CouncilPolicyResponse(10, new BigDecimal("0.6000"));
    when(policyService.get(councilId)).thenReturn(response);

    assertSame(response, policyFacade.getPolicy(councilId));
  }

  @Test
  void updatePolicy_권한이_없으면_ACCESS_DENIED() {
    UUID councilId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user =
        TestFixtures.user(
            userId,
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
            UserRole.STUDENT,
            Instant.parse("2026-05-01T00:00:00Z"));
    when(userService.getById(userId)).thenReturn(user);
    when(councilService.isAdmin(userId, councilId)).thenReturn(false);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> policyFacade.updatePolicy(councilId, userId, 10, new BigDecimal("0.7000")));

    assertSame(PolicyErrorCode.ACCESS_DENIED, exception.getErrorCode());
    verify(policyService, never()).update(councilId, 10, new BigDecimal("0.7000"));
  }

  @Test
  void updatePolicy_슈퍼유저면_수정한다() {
    UUID councilId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user =
        TestFixtures.user(
            userId,
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "컴퓨터공학과"),
            UserRole.SUPER,
            Instant.parse("2026-05-01T00:00:00Z"));
    when(userService.getById(userId)).thenReturn(user);

    policyFacade.updatePolicy(councilId, userId, 20, new BigDecimal("0.8000"));

    verify(policyService).update(councilId, 20, new BigDecimal("0.8000"));
  }
}
