package com.example.sejong_voice.council;

import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.council.repository.CouncilMemberRepository;
import com.example.sejong_voice.council.repository.CouncilRepository;
import com.example.sejong_voice.exception.BusinessException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouncilService {
  private final CouncilRepository councilRepository;
  private final CouncilMemberRepository councilMemberRepository;

  public List<CouncilResponse> getAll() {
    return councilRepository.findAll().stream()
        .map(CouncilResponse::new)
        .collect(Collectors.toList());
  }

  public Council getById(UUID id) {
    return councilRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CouncilErrorCode.COUNCIL_NOT_FOUND));
  }

  public boolean isAdmin(UUID userId, UUID councilId) {
    return councilMemberRepository.existsByUserIdAndCouncilId(userId, councilId);
  }
}
