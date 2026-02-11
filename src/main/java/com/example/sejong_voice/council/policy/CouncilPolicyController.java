package com.example.sejong_voice.council.policy;

import com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/councils/{councilId}/policies")
@RequiredArgsConstructor
public class CouncilPolicyController {

  private final CouncilPolicyService councilPolicyService;

  @GetMapping
  public ResponseEntity<CouncilPolicyResponse> get(@PathVariable UUID councilId) {
    return ResponseEntity.ok(councilPolicyService.get(councilId));
  }
}