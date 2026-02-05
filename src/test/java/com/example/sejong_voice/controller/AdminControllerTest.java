package com.example.sejong_voice.controller;

import com.example.sejong_voice.auth.AdminController;
import com.example.sejong_voice.auth.AuthService;
import com.example.sejong_voice.auth.User;
import com.example.sejong_voice.auth.UserRole;
import com.example.sejong_voice.auth.dto.UserRoleRequest;
import com.example.sejong_voice.auth.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // JwtUtil이 @PostConstruct에서 키 생성할 때 필요한 값
        "jwt.secret-key=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.issuer=test-issuer"
})
class AdminControllerSecurityIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean AuthService authService;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String tokenOf(String callerStudentNo, UserRole role) {
        User user = mock(User.class);
        when(user.getStudentNo()).thenReturn(callerStudentNo);
        when(user.getRole()).thenReturn(role);
        return jwtUtil.generateToken(user, 1000L * 60 * 10);
    }

    private void assertUnauthOrForbidden(int statusCode) {
        assertThat(statusCode).isIn(401, 403);
    }

    private void assertCallerPrincipalCaptured(ArgumentCaptor<UserRoleRequest> captor,
                                               String expectedStudentNo,
                                               UserRole expectedRole) {
        UserRoleRequest principal = captor.getValue();
        assertThat(principal).isNotNull();
        assertThat(principal.getStudentNo()).isEqualTo(expectedStudentNo);
        assertThat(principal.getRole()).isEqualTo(expectedRole);
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{studentNo}/department")
    class UpdateDepartmentTests {

        private final String url = "/api/v1/admin/users/{studentNo}/department";

        @Test
        @DisplayName("무토큰 → 401/403")
        void noToken_denied() throws Exception {
            var result = mockMvc.perform(patch(url, "21011570")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"컴퓨터공학과\""))
                    .andReturn();

            assertUnauthOrForbidden(result.getResponse().getStatus());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("ADMIN 토큰 → SUPER 전용이면 403")
        void adminToken_forbidden_ifSuperOnly() throws Exception {
            String token = tokenOf("99990000", UserRole.ADMIN);

            mockMvc.perform(patch(url, "21011570")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"department\":\"컴퓨터공학과\"}"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("SUPER 토큰 → 200 + principal 주입 + 서비스 호출 인자 검증")
        void superToken_ok_andPrincipalInjected() throws Exception {
            String token = tokenOf("99990000", UserRole.SUPER);

            mockMvc.perform(patch(url, "21011570")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"department\":\"컴퓨터공학과\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<UserRoleRequest> callerCaptor = ArgumentCaptor.forClass(UserRoleRequest.class);
//
//            verify(authService).updateDepartment(
//                    //callerCaptor.capture(),
//                    eq("21011570"),
//                    eq("{\"department\":\"컴퓨터공학과\"}")
//            );

            assertCallerPrincipalCaptured(callerCaptor, "99990000", UserRole.SUPER);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{studentNo}/status")
    class UpdateStatusTests {

        private final String url = "/api/v1/admin/users/{studentNo}/status";

        @Test
        @DisplayName("무토큰 → 401/403")
        void noToken_denied() throws Exception {
            var result = mockMvc.perform(patch(url, "21011570")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\",\"duration\":\"2026-02-04T12:00:00\"}"))
                    .andReturn();

            assertUnauthOrForbidden(result.getResponse().getStatus());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("ADMIN 토큰 → SUPER 전용이면 403")
        void adminToken_forbidden_ifSuperOnly() throws Exception {
            String token = tokenOf("99990000", UserRole.ADMIN);

            mockMvc.perform(patch(url, "21011570")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\",\"duration\":\"2026-02-04T12:00:00\"}"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("SUPER 토큰 → 200 + principal 주입 + 서비스 호출 확인")
        void superToken_ok_andPrincipalInjected() throws Exception {
            String token = tokenOf("99990000", UserRole.SUPER);

            mockMvc.perform(patch(url, "21011570")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\",\"duration\":\"2026-02-04T12:00:00\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<UserRoleRequest> callerCaptor = ArgumentCaptor.forClass(UserRoleRequest.class);

//            verify(authService).updateStatus(
//                    callerCaptor.capture(),
//                    eq("21011570"),
//                    any(),   // UserStatus
//                    any()    // LocalDateTime
//            );

            assertCallerPrincipalCaptured(callerCaptor, "99990000", UserRole.SUPER);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/user/{studentNo}/role")
    class UpdateRoleTests {

        private final String url = "/api/v1/admin/user/{studentNo}/role";

        @Test
        @DisplayName("무토큰 → 401/403")
        void noToken_denied() throws Exception {
            var result = mockMvc.perform(patch(url, "21011570")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\",\"studentNo\":\"21011570\"}"))
                    .andReturn();

            assertUnauthOrForbidden(result.getResponse().getStatus());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("ADMIN 토큰 → SUPER 전용이면 403")
        void adminToken_forbidden_ifSuperOnly() throws Exception {
            String token = tokenOf("99990000", UserRole.ADMIN);

            mockMvc.perform(patch(url, "21011570")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\",\"studentNo\":\"21011570\"}"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("SUPER 토큰 → 200 + principal 주입 + (newRole.getRole()) 전달 검증")
        void superToken_ok_andPrincipalInjected() throws Exception {
            String token = tokenOf("99990000", UserRole.SUPER);

            mockMvc.perform(patch(url, "21011570")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\",\"studentNo\":\"21011570\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<UserRoleRequest> callerCaptor = ArgumentCaptor.forClass(UserRoleRequest.class);

//            verify(authService).updateRole(
//                    callerCaptor.capture(),
//                    eq("21011570"),
//                    eq(UserRole.ADMIN)
//            );

            assertCallerPrincipalCaptured(callerCaptor, "99990000", UserRole.SUPER);
        }
    }
}
