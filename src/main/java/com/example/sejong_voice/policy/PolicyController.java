package com.example.sejong_voice.policy;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.policy.dto.CouncilPolicyResponse;
import com.example.sejong_voice.policy.dto.CouncilPolicyUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/councils/{councilId}/policies")
@RequiredArgsConstructor
public class PolicyController {

  private final PolicyFacade policyFacade;

  @GetMapping
  public ResponseEntity<CouncilPolicyResponse> get(@PathVariable UUID councilId) {
    return ResponseEntity.ok(policyFacade.getPolicy(councilId));
  }

  @PutMapping
  public ResponseEntity<Void> update(
      @PathVariable UUID councilId,
      @RequestBody @Valid CouncilPolicyUpdateRequest request,
      @AuthUser UserPrincipal userPrincipal
  ) {
    policyFacade.updatePolicy(
        councilId,
        userPrincipal.getUserId(),
        request.minVote(),
        request.approveRatioThreshold()
    );
    return ResponseEntity.ok().build();
  }
}