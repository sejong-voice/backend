package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.DepartmentRequest;
import com.example.sejong_voice.auth.dto.RoleUpdateRequest;
import com.example.sejong_voice.auth.dto.UserStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AuthService authService;

  // 학과 수정 -> 만약에 전과를 하면 수정하는 방식으로 하려고 했는데, 사실 전과하면 sso에서 mayjor값이 변경되기 때문에 안써도 되긴함
  @PatchMapping("/user/{studentNo}/department")
  public ResponseEntity<Void> updateDepartment(
      @PathVariable String studentNo, //targetUser
      @Valid @RequestBody DepartmentRequest newDepartment) {

    authService.updateDepartment(studentNo, newDepartment);
    return ResponseEntity.ok().build();
  }

  //상태 변경
  @PatchMapping("/user/{studentNo}/status")
  public ResponseEntity<Void> updateStatus(
      @PathVariable String studentNo, //targetUser
      @RequestBody UserStatusRequest newStatus) {
    authService.updateStatus(studentNo, newStatus.getStatus(), newStatus.getDuration());
    return ResponseEntity.ok().build();
  }

  //조기 복구
  @PatchMapping("/user/{studentNo}/restore")
  public ResponseEntity<Void> activateStatus(@PathVariable String studentNo) {
    authService.activateStatus(studentNo);
    return ResponseEntity.ok().build();
  }

  //권한 수정
  @PatchMapping("/user/{studentNo}/role")
  public ResponseEntity<Void> updateRole(
      @PathVariable String studentNo,     //targetUser
      @Valid @RequestBody RoleUpdateRequest request
  ) {
    authService.updateRole(studentNo, request.getRole());
    return ResponseEntity.ok().build();
  }
}
