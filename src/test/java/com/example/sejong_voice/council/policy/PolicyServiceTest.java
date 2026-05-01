package com.example.sejong_voice.council.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.council.CouncilService;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

  @Mock private CouncilService councilService;

  private PolicyService policyService;
  private Clock fixedClock;
  private Instant now;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    policyService = new PolicyService(councilService, fixedClock);
    now = Instant.now(fixedClock);
  }

  @Test
  void get_학생회정책을_응답으로_반환한다() {
    Affiliation affiliation = TestFixtures.affiliation(UUID.randomUUID(), "총학생회");
    User admin =
        TestFixtures.user(UUID.randomUUID(), "20201234", "홍길동", affiliation, UserRole.STUDENT, now);
    Council council =
        TestFixtures.council(
            affiliation.getId(), affiliation, admin, 10, new BigDecimal("0.6000"), now);
    when(councilService.getById(council.getId())).thenReturn(council);

    CouncilPolicyResponse result = policyService.get(council.getId());

    assertEquals(10, result.minVote());
    assertEquals(new BigDecimal("0.6000"), result.approveRatioThreshold());
  }

  @Test
  void update_학생회정책을_변경한다() {
    Affiliation affiliation = TestFixtures.affiliation(UUID.randomUUID(), "총학생회");
    User admin =
        TestFixtures.user(UUID.randomUUID(), "20201234", "홍길동", affiliation, UserRole.STUDENT, now);
    Council council =
        TestFixtures.council(
            affiliation.getId(), affiliation, admin, 10, new BigDecimal("0.6000"), now);
    when(councilService.getById(council.getId())).thenReturn(council);

    policyService.update(council.getId(), 30, new BigDecimal("0.7500"));

    assertEquals(30, council.getMinVote());
    assertEquals(new BigDecimal("0.7500"), council.getApproveRatioThreshold());
    assertEquals(now, council.getUpdatedAt());
    verify(councilService).getById(council.getId());
  }
}
