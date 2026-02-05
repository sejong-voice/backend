package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.DepartmentRequest;
import com.example.sejong_voice.auth.dto.UserRoleRequest;
import com.example.sejong_voice.auth.dto.UserStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    //학과 수정 -> 만약에 전과를 하면 수정하는 방식으로 하려고 했는데, 사실 전과하면 sso에서 mayjor값이 변경되기 때문에 안써도 되긴함
    @PatchMapping("/user/{studentNo}/department")
    public ResponseEntity<Void> updateDepartment(
            //@AuthenticationPrincipal UserRoleRequest superRole,
            @PathVariable String studentNo,
            @Valid @RequestBody DepartmentRequest newDepartment){
        authService.updateDepartment( studentNo, newDepartment);
        return ResponseEntity.ok().build();
    }

    //상태 변경
    @PatchMapping("/user/{studentNo}/status")
    public ResponseEntity<Void> updateStatus(
            //@AuthenticationPrincipal UserRoleRequest superRole,//securityFilter에서 권한 검증하니까 굳이 안해도됨
            @PathVariable String studentNo,
            @RequestBody UserStatusRequest newStatus){
        authService.updateStatus( studentNo,newStatus.getStatus(),newStatus.getDuration());
        return ResponseEntity.ok().build();
    }
    //권한 수정
    @PatchMapping("/user/{studentNo}/role")
    public ResponseEntity<Void> updateRole(
            //@AuthenticationPrincipal UserRoleRequest superRole, // 호출자의 권한 정보
            @PathVariable String studentNo,     //targetUser 학번
            @RequestBody UserRoleRequest newRole){//targetUser의 변경될 권한 dto
            authService.updateRole( studentNo, newRole.getRole());
            return ResponseEntity.ok().build();
    }
}
