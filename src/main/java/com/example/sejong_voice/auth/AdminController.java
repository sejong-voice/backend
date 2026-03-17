package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.DepartmentRequest;
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

  // 학과 수정 -> 만약에 전과를 하면 수정하는 방식으로 하려고 했는데, 사실 전과하면 sso에서 mayjor값이 변경되기 때문에 안써도 되긴함
  @Operation(
      summary = "사용자 학과 변경",
      description = "관리자가 특정 사용자의 학과 정보를 변경합니다. 현재 사용하지 않는 api"
  )
  @PatchMapping("/user/{studentNo}/department")
  public ResponseEntity<Void> updateDepartment(
      @PathVariable String studentNo, //targetUser
      @Valid @RequestBody DepartmentRequest newDepartment) {

    authService.updateDepartment(studentNo, newDepartment);
    return ResponseEntity.ok().build();
  }

  //상태 변경
  @Operation(
      summary = "사용자 상태 변경",
      description = "관리자가 특정 사용자의 상태(정지 등)와 기간을 변경합니다. 계정 제재에 사용. ACTIVE(활동 상태), SUSPENDED(기한 정지), WITHDRAWN(영구정지) "
  )
  @PatchMapping("/user/{studentNo}/status")
  public ResponseEntity<Void> updateStatus(
      @PathVariable String studentNo, //targetUser
      @RequestBody UserStatusRequest newStatus) {
    authService.updateStatus(studentNo, newStatus.getStatus(), newStatus.getDuration());
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
