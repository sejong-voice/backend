package com.example.sejong_voice.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sejong_voice.auth.dto.DepartmentRequest;
import com.example.sejong_voice.exception.GlobalExceptionHandler;
import com.example.sejong_voice.user.entity.UserRole;
import com.example.sejong_voice.user.entity.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-21T00:00:00Z"), ZoneOffset.UTC);

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(authService))
                .setControllerAdvice(new GlobalExceptionHandler(fixedClock))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator())
                .build();
    }


    @Test
    void updateStatus_성공_200_서비스호출() throws Exception {
        String studentNo = "20201234";

        mockMvc.perform(patch("/api/v1/admin/user/{studentNo}/status", studentNo)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"status":"SUSPENDED","durationDays":7}
                """))
                .andExpect(status().isOk());

        verify(authService).updateStatus(studentNo, UserStatus.SUSPENDED, 7);
    }

    @Test
    void restore_성공_200_서비스호출() throws Exception {
        String studentNo = "20201234";

        mockMvc.perform(patch("/api/v1/admin/user/{studentNo}/restore", studentNo))
                .andExpect(status().isOk());

        verify(authService).activateStatus(studentNo);
    }

    @Test
    void updateRole_성공_200_서비스호출() throws Exception {
        String studentNo = "20201234";

        mockMvc.perform(patch("/api/v1/admin/user/{studentNo}/role", studentNo)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"role":"SUPER"}
                """))
                .andExpect(status().isOk());

        verify(authService).updateRole(studentNo, UserRole.SUPER);
    }

    @Test
    void updateStatus_status_null이면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/user/{studentNo}/status", "20201234")
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"durationDays":7}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(authService);
    }

    @Test
    void updateStatus_durationDays_null이면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/user/{studentNo}/status", "20201234")
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"status":"SUSPENDED"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(authService);
    }

    @Test
    void updateStatus_durationDays_음수면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/user/{studentNo}/status", "20201234")
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"status":"SUSPENDED","durationDays":-1}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(authService);
    }

    @Test
    void updateRole_role_null이면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/user/{studentNo}/role", "20201234")
                        .contentType(APPLICATION_JSON)
                        .content("""
                {}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(authService);
    }


    private Validator validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.afterPropertiesSet();
        return bean;
    }
}
