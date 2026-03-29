package com.example.sejong_voice.council;

import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.council.repository.CouncilMemberRepository;
import com.example.sejong_voice.council.repository.CouncilRepository;
import com.example.sejong_voice.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouncilService {

  private final CouncilRepository councilRepository;
  private final CouncilMemberRepository councilMemberRepository;

  public List<CouncilResponse> getAll(String keyword) {
    List<Council> councils = StringUtils.hasText(keyword)
        ? councilRepository.findByNameContaining(keyword)
        : councilRepository.findAll();
    return buildOrderedResponses(councils);
  }

  private List<CouncilResponse> buildOrderedResponses(List<Council> councils) {
    Map<UUID, List<Council>> childrenByParentId = new HashMap<>();
    List<Council> rootList = new ArrayList<>();
    Map<UUID, Council> councilById = new HashMap<>();

    for (Council council : councils) {
      councilById.put(council.getId(), council);
    }

    for (Council council : councils) {
      Council parent = council.getParent();
      if (parent == null || !councilById.containsKey(parent.getId())) {
        rootList.add(council);
        continue;
      }

      childrenByParentId
          .computeIfAbsent(parent.getId(), ignored -> new ArrayList<>())
          .add(council);
    }

    List<CouncilResponse> orderedResponses = new ArrayList<>();
    for (Council root : rootList) {
      addCouncilWithDescendants(root, childrenByParentId, orderedResponses);
    }

    return orderedResponses;
  }

  public Council getById(UUID id) {
    return councilRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CouncilErrorCode.COUNCIL_NOT_FOUND));
  }

  public boolean isAdmin(UUID userId, UUID councilId) {
    return councilMemberRepository.existsByUserIdAndCouncilId(userId, councilId);
  }

  public boolean hasManagedCouncil(UUID userId) {
    return councilMemberRepository.existsByUserId(userId);
  }

  private void addCouncilWithDescendants(
      Council council,
      Map<UUID, List<Council>> childrenByParentId,
      List<CouncilResponse> orderedResponses
  ) {
    orderedResponses.add(new CouncilResponse(council));

    for (Council child : childrenByParentId.getOrDefault(council.getId(), List.of())) {
      addCouncilWithDescendants(child, childrenByParentId, orderedResponses);
    }
  }
}
