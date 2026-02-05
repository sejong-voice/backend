package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.UserRoleRequest;
import com.example.sejong_voice.auth.dto.UserStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    //학과 수정
    @PatchMapping("/users/{studentNo}/department")
    public ResponseEntity<Void> updateDepartment(
            @AuthenticationPrincipal UserRoleRequest superRole,
            @PathVariable String studentNo,
            @RequestBody String newDepartment){
        authService.updateDepartment(superRole, studentNo, newDepartment);
        return ResponseEntity.ok().build();
    }

    //상태 변경
    @PatchMapping("/users/{studentNo}/status")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal UserRoleRequest superRole,
            @PathVariable String studentNo,
            @RequestBody UserStatusRequest newStatus){
        authService.updateStatus(superRole, studentNo,newStatus.getStatus(),newStatus.getDuration());
        return ResponseEntity.ok().build();
    }
    //권한 수정
    @PatchMapping("/user/{studentNo}/role")
    public ResponseEntity<Void> updateRole(
            @AuthenticationPrincipal UserRoleRequest superRole, // 호출자의 권한 정보
            @PathVariable String studentNo,     //targetUser 학번
            @RequestBody UserRoleRequest newRole){//targetUser의 변경될 권한 dto
            authService.updateRole(superRole, studentNo, newRole.getRole());
            return ResponseEntity.ok().build();
    }
}
