package com.example.sejong_voice.resolver;

import com.example.sejong_voice._common.security.UserPrincipalArgumentResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.method.support.ModelAndViewContainer;

public class UserPrincipalArgumentResolverTest {

    private UserPrincipalArgumentResolver resolver;
    private final ModelAndViewContainer mavContainer = new ModelAndViewContainer();
    private final WebDataBinderFactory binderFactory = null; // 이 테스트에선 사용 안 함

    @BeforeEach
    void setUp() {
        resolver = new UserPrincipalArgumentResolver();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
