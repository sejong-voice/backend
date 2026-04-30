package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.RoleUpdateRequest;
import com.example.sejong_voice.auth.dto.UserStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "관리자 API", description = "관리자 권한으로 사용자 정보/상태/권한을 관리하는 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AuthService authService;


  //상태 변경
  @Operation(
      summary = "사용자 상태 변경",
      description = "관리자가 특정 사용자의 상태(정지 등)와 기간을 day를 기준으로 입력 받습니다. Suspend,Withdraw,Quit에만 사용가능. Active는 반영안됨 계정 제재에 사용.Quit의 경우에는 관리자가 임의로 특정 유저를 탈퇴처리 해줘야할 경우에 대비하여 추가해둠. ACTIVE(활동 상태), SUSPENDED(기한 정지), WITHDRAWN(영구정지) "
  )
  @PatchMapping("/user/{studentNo}/status")
  public ResponseEntity<Void> updateStatus(
      @PathVariable String studentNo, //targetUser
      @Valid @RequestBody UserStatusRequest newStatus) {
    authService.updateStatus(studentNo, newStatus.getStatus(), newStatus.getDurationDays());
    return ResponseEntity.ok().build();
  }

  //조기 복구
  @Operation(
      summary = "사용자 상태 조기 복구",
      description = "관리자가 특정 사용자의 제재 상태를 즉시 복구합니다. SUSPENDED, WITHDRAW에서 ACTIVE로 복구. 재제 기한(duration)도 초기화"
  )
  @PatchMapping("/user/{studentNo}/restore")
  public ResponseEntity<Void> activateStatus(@PathVariable String studentNo) {
    authService.activateStatus(studentNo);
    return ResponseEntity.ok().build();
  }

  //권한 수정
  @Operation(
      summary = "사용자 권한 변경",
      description = "관리자가 특정 사용자의 시스템 권한을 변경합니다. STUDENT(일반 유저), ADMIN(학생회 계정), SUPER(관리자 계정)"
  )
  @PatchMapping("/user/{studentNo}/role")
  public ResponseEntity<Void> updateRole(
      @PathVariable String studentNo,     //targetUser
      @Valid @RequestBody RoleUpdateRequest request
  ) {
    authService.updateRole(studentNo, request.getRole());
    return ResponseEntity.ok().build();
  }
}
