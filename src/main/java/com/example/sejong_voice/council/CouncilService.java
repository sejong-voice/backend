package com.example.sejong_voice.council;

import com.example.sejong_voice.affiliation.Affiliation;
import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.council.entity.Council;
import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.council.repository.CouncilRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.entity.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  public List<CouncilResponse> getAll(String keyword) {
    List<Council> councils =
        StringUtils.hasText(keyword)
            ? councilRepository.findByNameContainingIgnoreCaseWithParent(keyword)
            : councilRepository.findAllWithParent();
    return buildOrderedResponses(councils);
  }

  public List<CouncilResponse> getAccessible(String keyword, User user) {
    List<Council> councils =
        StringUtils.hasText(keyword)
            ? councilRepository.findByNameContainingIgnoreCaseWithParent(keyword)
            : councilRepository.findAllWithParent();
    Set<UUID> accessibleAffiliationIds = getAccessibleAffiliationIds(user);

    return buildOrderedResponses(
        councils.stream()
            .filter(council -> accessibleAffiliationIds.contains(council.getAffiliation().getId()))
            .toList());
  }

  private List<CouncilResponse> buildOrderedResponses(List<Council> councils) {
    Map<UUID, Council> councilByAffiliationId = new HashMap<>();
    Map<UUID, List<Council>> childrenByParentAffiliationId = new HashMap<>();
    List<Council> rootList = new ArrayList<>();

    for (Council council : councils) {
      councilByAffiliationId.put(council.getAffiliation().getId(), council);
    }

    for (Council council : councils) {
      Affiliation parentAffiliation = council.getAffiliation().getParent();
      if (parentAffiliation == null) {
        rootList.add(council);
        continue;
      }

      Council parentCouncil = councilByAffiliationId.get(parentAffiliation.getId());
      if (parentCouncil == null) {
        rootList.add(council);
        continue;
      }

      childrenByParentAffiliationId
          .computeIfAbsent(parentAffiliation.getId(), ignored -> new ArrayList<>())
          .add(council);
    }

    List<CouncilResponse> orderedResponses = new ArrayList<>();
    for (Council root : rootList) {
      addCouncilWithDescendants(root, childrenByParentAffiliationId, orderedResponses);
    }

    return orderedResponses;
  }

  public Council getById(UUID id) {
    return councilRepository
        .findById(id)
        .orElseThrow(() -> new BusinessException(CouncilErrorCode.COUNCIL_NOT_FOUND));
  }

  public boolean isAdmin(UUID userId, UUID councilId) {
    return getById(councilId).isAdmin(userId);
  }

  public boolean hasManagedCouncil(UUID userId) {
    return councilRepository.existsByAdminUserId(userId);
  }

  public boolean hasAccessToCouncil(User user, Council council) {
    return getAccessibleAffiliationIds(user).contains(council.getAffiliation().getId());
  }

  private Set<UUID> getAccessibleAffiliationIds(User user) {
    Set<UUID> accessibleAffiliationIds = new HashSet<>();
    Affiliation current = user.getAffiliation();

    while (current != null) {
      accessibleAffiliationIds.add(current.getId());
      current = current.getParent();
    }

    return accessibleAffiliationIds;
  }

  private void addCouncilWithDescendants(
      Council council,
      Map<UUID, List<Council>> childrenByParentAffiliationId,
      List<CouncilResponse> orderedResponses) {
    orderedResponses.add(new CouncilResponse(council));

    for (Council child :
        childrenByParentAffiliationId.getOrDefault(council.getAffiliation().getId(), List.of())) {
      addCouncilWithDescendants(child, childrenByParentAffiliationId, orderedResponses);
    }
  }
}
