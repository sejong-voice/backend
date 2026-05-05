package com.example.sejong_voice.council.policy;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.council.policy.dto.CouncilPolicyResponse;
import com.example.sejong_voice.council.policy.dto.CouncilPolicyUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "정책 API", description = "학생회 정책 조회/수정 API")
@RestController
@RequestMapping("/api/v1/councils/{councilId}/policies")
@RequiredArgsConstructor
public class PolicyController {

  private final PolicyFacade policyFacade;

  @Operation(summary = "학생회 정책 조회", description = "특정 학생회의 현재 정책 정보를 조회합니다.")
  @GetMapping
  public ResponseEntity<CouncilPolicyResponse> get(@PathVariable UUID councilId) {
    return ResponseEntity.ok(policyFacade.getPolicy(councilId));
  }

  @Operation(
      summary = "학생회 정책 수정",
      description = "학생회의 최소 투표 수/가결 비율 임계값 정책을 수정합니다. 학과, 학부별로 인원이 달라서 임의로 조절을 해주는걸로 협의를 봄")
  @PutMapping
  public ResponseEntity<Void> update(
      @PathVariable UUID councilId,
      @RequestBody @Valid CouncilPolicyUpdateRequest request,
      @AuthUser UserPrincipal userPrincipal) {
    policyFacade.updatePolicy(
        councilId, userPrincipal.getUserId(), request.minVote(), request.approveRatioThreshold());
    return ResponseEntity.ok().build();
  }
}
