package com.example.sejong_voice.council;

import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouncilFacade {

  private final CouncilService councilService;
  private final UserService userService;

  @Transactional(readOnly = true)
  public List<CouncilResponse> getCouncils(String keyword, Boolean accessible, UUID userId) {
    if (!Boolean.TRUE.equals(accessible)) {
      return councilService.getAll(keyword);
    }

    if (userId == null) {
      throw new BusinessException(CouncilErrorCode.ACCESS_DENIED);
    }

    return councilService.getAccessible(keyword, userService.getById(userId));
  }
}
