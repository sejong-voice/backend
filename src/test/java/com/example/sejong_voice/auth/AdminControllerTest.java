//package com.example.sejong_voice.auth;
//
//import com.example.sejong_voice.auth.dto.UserPrincipal;
//import com.example.sejong_voice.user.entity.User;
//import com.example.sejong_voice.user.entity.UserRole;
//import com.example.sejong_voice.auth.jwt.JwtUtil;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@TestPropertySource(properties = {
//    // JwtUtil이 @PostConstruct에서 키 생성할 때 필요한 값
//    "jwt.secret-key=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
//    "jwt.issuer=test-issuer"
//})
//class AdminControllerSecurityIT {
//
//  @Autowired
//  MockMvc mockMvc;
//  @Autowired
//  JwtUtil jwtUtil;
//
//  @MockitoBean
//  AuthService authService;
//
//  private String bearer(String token) {
//    return "Bearer " + token;
//  }
//
//  private String tokenOf(String callerStudentNo, UserRole role) {
//    User user = mock(User.class);
//    when(user.getStudentNo()).thenReturn(callerStudentNo);
//    when(user.getRole()).thenReturn(role);
//    return jwtUtil.generateToken(user, 1000L * 60 * 10);
//  }
//
//  private void assertUnauthOrForbidden(int statusCode) {
//    assertThat(statusCode).isIn(401, 403);
//  }
//
//  private void assertCallerPrincipalCaptured(ArgumentCaptor<UserPrincipal> captor,
//      String expectedStudentNo,
//      UserRole expectedRole) {
//    UserPrincipal principal = captor.getValue();
//    assertThat(principal).isNotNull();
//    assertThat(principal.getStudentNo()).isEqualTo(expectedStudentNo);
//    assertThat(principal.getRole()).isEqualTo(expectedRole);
//  }
//
//  @Nested
//  @DisplayName("PATCH /api/v1/admin/user/{studentNo}/department")
//  class UpdateDepartmentTests {
//
//    private final String url = "/api/v1/admin/user/{studentNo}/department";
//
//    @Test
//    @DisplayName("무토큰 → 401/403")
//    void noToken_denied() throws Exception {
//      var result = mockMvc.perform(patch(url, "21011570")
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"department\":\"컴퓨터공학과\"}"))
//          .andReturn();
//
//      assertUnauthOrForbidden(result.getResponse().getStatus());
//      verifyNoInteractions(authService);
//    }
//
//    @Test
//    @DisplayName("ADMIN 토큰 → (현재 보안 설정 기준) 200 허용")
//    void adminToken_allowed_withCurrentSecurityConfig() throws Exception {
//      String token = tokenOf("99990000", UserRole.ADMIN);
//
//      mockMvc.perform(patch(url, "21011570")
//              .header("Authorization", bearer(token))
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"department\":\"컴퓨터공학과\"}"))
//          .andExpect(status().isForbidden());
//
//      // SUPER 전용으로 막고 싶다면 SecurityConfig에서
//      // .requestMatchers("/api/v1/admin/user/*/department").hasRole("SUPER")
//      // 처럼 경로를 맞춰줘야 하고, 그때 이 테스트 기대값을 403으로 바꾸면 됩니다.
//    }
//
//    @Test
//    @DisplayName("SUPER 토큰 → 200")
//    void superToken_ok() throws Exception {
//      String token = tokenOf("99990000", UserRole.SUPER);
//
//      mockMvc.perform(patch(url, "21011570")
//              .header("Authorization", bearer(token))
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"department\":\"컴퓨터공학과\"}"))
//          .andExpect(status().isOk());
//
//      // 필요하면 여기서 authService.updateDepartment(...) 호출 여부를 verify 할 수 있습니다.
//      // 현재 컨트롤러 시그니처는 principal을 받지 않으므로 principal 캡처(assertCallerPrincipalCaptured)는 제거하는 게 자연스럽습니다.
//    }
//  }
//
//  @Nested
//  @DisplayName("PATCH /api/v1/admin/user/{studentNo}/status")
//  class UpdateStatusTests {
//
//    private final String url = "/api/v1/admin/user/{studentNo}/status";
//
//    @Test
//    @DisplayName("무토큰 → 401/403")
//    void noToken_denied() throws Exception {
//      var result = mockMvc.perform(patch(url, "21011570")
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"status\":\"ACTIVE\",\"duration\":\"2026-02-04T12:00:00\"}"))
//          .andReturn();
//
//      assertUnauthOrForbidden(result.getResponse().getStatus());
//      verifyNoInteractions(authService);
//    }
//
//    @Test
//    @DisplayName("ADMIN 토큰 → SUPER 전용이면 403")
//    void adminToken_forbidden_ifSuperOnly() throws Exception {
//      String token = tokenOf("99990000", UserRole.ADMIN);
//
//      mockMvc.perform(patch(url, "21011570")
//              .header("Authorization", bearer(token))
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"status\":\"ACTIVE\",\"duration\":\"2026-02-04T12:00:00\"}"))
//          .andExpect(status().isForbidden());
//
//      verifyNoInteractions(authService);
//    }
//
////    @Test
////    @DisplayName("SUPER 토큰 → 200 + principal 주입 + 서비스 호출 확인")
////    void superToken_ok_andPrincipalInjected() throws Exception {
////      String token = tokenOf("99990000", UserRole.SUPER);
////
////      mockMvc.perform(patch(url, "21011570")
////              .header("Authorization", bearer(token))
////              .contentType(MediaType.APPLICATION_JSON)
////              .content("{\"status\":\"ACTIVE\",\"duration\":\"2026-02-04T12:00:00\"}"))
////          .andExpect(status().isOk());
////
////      ArgumentCaptor<UserRoleRequest> callerCaptor = ArgumentCaptor.forClass(UserRoleRequest.class);
////
////            verify(authService).updateStatus(
////                  //  callerCaptor.capture(),
////                    eq("21011570"),
////                    any(),   // UserStatus
////                    any()    // LocalDateTime
////            );
////
////      assertCallerPrincipalCaptured(callerCaptor, "99990000", UserRole.SUPER);
////    }
//  }
//
//  @Nested
//  @DisplayName("PATCH /api/v1/admin/user/{studentNo}/role")
//  class UpdateRoleTests {
//
//    private final String url = "/api/v1/admin/user/{studentNo}/role";
//
//    @Test
//    @DisplayName("무토큰 → 401/403")
//    void noToken_denied() throws Exception {
//      var result = mockMvc.perform(patch(url, "21011570")
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"role\":\"ADMIN\",\"studentNo\":\"21011570\"}"))
//          .andReturn();
//
//      assertUnauthOrForbidden(result.getResponse().getStatus());
//      verifyNoInteractions(authService);
//    }
//
//    @Test
//    @DisplayName("ADMIN 토큰 → SUPER 전용이면 403")
//    void adminToken_forbidden_ifSuperOnly() throws Exception {
//      String token = tokenOf("99990000", UserRole.ADMIN);
//
//      mockMvc.perform(patch(url, "21011570")
//              .header("Authorization", bearer(token))
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"role\":\"ADMIN\",\"studentNo\":\"21011570\"}"))
//          .andExpect(status().isForbidden());
//
//      verifyNoInteractions(authService);
//    }
//
//    /*
//    @Test
//    @DisplayName("SUPER 토큰 → 200 + principal 주입 + (newRole.getRole()) 전달 검증")
//    void superToken_ok_andPrincipalInjected() throws Exception {
//      String token = tokenOf("99990000", UserRole.SUPER);
//
//      mockMvc.perform(patch(url, "21011570")
//              .header("Authorization", bearer(token))
//              .contentType(MediaType.APPLICATION_JSON)
//              .content("{\"role\":\"ADMIN\",\"studentNo\":\"21011570\"}"))
//          .andExpect(status().isOk());
//
//      ArgumentCaptor<UserRoleRequest> callerCaptor = ArgumentCaptor.forClass(UserRoleRequest.class);
//
////            verify(authService).updateRole(
////                    callerCaptor.capture(),
////                    eq("21011570"),
////                    eq(UserRole.ADMIN)
////            );
//
//      assertCallerPrincipalCaptured(callerCaptor, "99990000", UserRole.SUPER);
//    }
//    */
//  }
//}
