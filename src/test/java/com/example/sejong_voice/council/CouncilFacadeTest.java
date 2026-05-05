package com.example.sejong_voice.council;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.support.TestFixtures;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.entity.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouncilFacadeTest {

  @Mock private CouncilService councilService;
  @Mock private UserService userService;

  private CouncilFacade councilFacade;

  @BeforeEach
  void setUp() {
    councilFacade = new CouncilFacade(councilService, userService);
  }

  @Test
  void accessible가_false면_전체목록을_조회한다() {
    List<CouncilResponse> responses = List.of(new CouncilResponse(UUID.randomUUID(), "총학생회"));
    when(councilService.getAll("총")).thenReturn(responses);

    List<CouncilResponse> result = councilFacade.getCouncils("총", false, null);

    assertSame(responses, result);
    verify(councilService).getAll("총");
  }

  @Test
  void accessible가_true인데_로그인사용자가_없으면_ACCESS_DENIED() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> councilFacade.getCouncils(null, true, null));

    assertSame(CouncilErrorCode.ACCESS_DENIED, exception.getErrorCode());
  }

  @Test
  void accessible가_true면_접근가능한_목록을_조회한다() {
    UUID userId = UUID.randomUUID();
    User user =
        TestFixtures.user(
            userId,
            "20201234",
            "홍길동",
            TestFixtures.affiliation(UUID.randomUUID(), "공과대학"),
            UserRole.STUDENT,
            Instant.parse("2026-05-01T00:00:00Z"));
    List<CouncilResponse> responses = List.of(new CouncilResponse(UUID.randomUUID(), "공과대학"));
    when(userService.getById(userId)).thenReturn(user);
    when(councilService.getAccessible("공", user)).thenReturn(responses);

    List<CouncilResponse> result = councilFacade.getCouncils("공", true, userId);

    assertEquals(responses, result);
  }
}
