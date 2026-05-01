package com.example.sejong_voice.council;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.council.repository.CouncilRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouncilServiceTest {

  @Mock private CouncilRepository councilRepository;

  private CouncilService councilService;
  private Instant now;

  @BeforeEach
  void setUp() {
    councilService = new CouncilService(councilRepository);
    now = Instant.parse("2026-05-01T00:00:00Z");
  }

  @Test
  void getAll_부모뒤에_자식순으로_정렬한다() {
    Affiliation rootAff = TestFixtures.affiliation(UUID.randomUUID(), "총학생회");
    Affiliation childAff = TestFixtures.affiliation(UUID.randomUUID(), "공과대학", rootAff);
    User admin =
        TestFixtures.user(UUID.randomUUID(), "20201234", "홍길동", rootAff, UserRole.STUDENT, now);
    Council child =
        TestFixtures.council(childAff.getId(), childAff, admin, 10, new BigDecimal("0.5000"), now);
    Council root =
        TestFixtures.council(rootAff.getId(), rootAff, admin, 10, new BigDecimal("0.5000"), now);
    when(councilRepository.findAllWithParent()).thenReturn(List.of(child, root));

    List<CouncilResponse> result = councilService.getAll(null);

    assertEquals(
        List.of(root.getId(), child.getId()), result.stream().map(CouncilResponse::id).toList());
  }

  @Test
  void getAccessible_본인소속과_상위학생회만_반환한다() {
    Affiliation rootAff = TestFixtures.affiliation(UUID.randomUUID(), "총학생회");
    Affiliation childAff = TestFixtures.affiliation(UUID.randomUUID(), "공과대학", rootAff);
    Affiliation otherAff = TestFixtures.affiliation(UUID.randomUUID(), "인문대학");
    User admin =
        TestFixtures.user(UUID.randomUUID(), "20201234", "홍길동", rootAff, UserRole.STUDENT, now);
    User member =
        TestFixtures.user(UUID.randomUUID(), "20209999", "김학생", childAff, UserRole.STUDENT, now);
    Council root =
        TestFixtures.council(rootAff.getId(), rootAff, admin, 10, new BigDecimal("0.5000"), now);
    Council child =
        TestFixtures.council(childAff.getId(), childAff, admin, 10, new BigDecimal("0.5000"), now);
    Council other =
        TestFixtures.council(otherAff.getId(), otherAff, admin, 10, new BigDecimal("0.5000"), now);
    when(councilRepository.findAllWithParent()).thenReturn(List.of(root, child, other));

    List<CouncilResponse> result = councilService.getAccessible(null, member);

    assertEquals(
        List.of(root.getId(), child.getId()), result.stream().map(CouncilResponse::id).toList());
  }

  @Test
  void getById_없으면_COUNCIL_NOT_FOUND() {
    UUID councilId = UUID.randomUUID();
    when(councilRepository.findById(councilId)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> councilService.getById(councilId));

    assertSame(CouncilErrorCode.COUNCIL_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void isAdmin_관리자여부를_반환한다() {
    Affiliation affiliation = TestFixtures.affiliation(UUID.randomUUID(), "총학생회");
    User admin =
        TestFixtures.user(UUID.randomUUID(), "20201234", "홍길동", affiliation, UserRole.STUDENT, now);
    Council council =
        TestFixtures.council(
            affiliation.getId(), affiliation, admin, 10, new BigDecimal("0.5000"), now);
    when(councilRepository.findById(council.getId())).thenReturn(Optional.of(council));

    assertTrue(councilService.isAdmin(admin.getId(), council.getId()));
    assertFalse(councilService.isAdmin(UUID.randomUUID(), council.getId()));
  }

  @Test
  void hasManagedCouncil_리포지토리결과를_반환한다() {
    UUID userId = UUID.randomUUID();
    when(councilRepository.existsByAdminUserId(userId)).thenReturn(true);

    assertTrue(councilService.hasManagedCouncil(userId));
  }
}
