package com.example.sejong_voice.resolver;

import static org.junit.jupiter.api.Assertions.*;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice._common.security.UserPrincipalArgumentResolver;
import com.example.sejong_voice.user.entity.UserRole;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class UserPrincipalArgumentResolverTest {

  private UserPrincipalArgumentResolver resolver;
  private final ModelAndViewContainer mavContainer = new ModelAndViewContainer();
  private final WebDataBinderFactory binderFactory = null; // 이 테스트에선 사용 안 함

  private MethodParameter parameter(String methodName, Class<?> paramType) throws Exception {
    Method method = DummyController.class.getDeclaredMethod(methodName, paramType);
    return new MethodParameter(method, 0);
  }

  private NativeWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  // 테스트용 더미 시그니처
  static class DummyController {
    void withAuthUser(@AuthUser UserPrincipal principal) {}

    void withoutAuthUser(UserPrincipal principal) {}

    void authUserButWrongType(@AuthUser String principal) {}
  }

  @BeforeEach
  void setUp() {
    resolver = new UserPrincipalArgumentResolver();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void supportsParameter_어노테이션과타입이맞으면_true() throws Exception {
    MethodParameter parameter = parameter("withAuthUser", UserPrincipal.class);
    assertTrue(resolver.supportsParameter(parameter));
  }

  @Test
  void supportsParameter_어노테이션없으면_false() throws Exception {
    MethodParameter parameter = parameter("withoutAuthUser", UserPrincipal.class);
    assertFalse(resolver.supportsParameter(parameter));
  }

  @Test
  void supportsParameter_타입이다르면_false() throws Exception {
    MethodParameter parameter = parameter("authUserButWrongType", String.class);
    assertFalse(resolver.supportsParameter(parameter));
  }

  @Test
  void resolveArgument_principal이UserPrincipal이면_해당객체반환() throws Exception {
    UserPrincipal principal =
        UserPrincipal.builder()
            .userId(UUID.randomUUID())
            .studentNo("20201234")
            .role(UserRole.STUDENT)
            .build();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "token")
            // 즉, 필터/리졸버가 SecurityContext에서 사용자 정보를 꺼낼 수 있게 미리 넣어두는 준비 코드
            );

    Object result =
        resolver.resolveArgument(
            parameter("withAuthUser", UserPrincipal.class),
            mavContainer,
            webRequest(),
            binderFactory);

    assertTrue(result instanceof UserPrincipal);
    assertSame(principal, (UserPrincipal) result);
  }

  @Test
  void resolveArgument_인증정보없으면_null() throws Exception {
    SecurityContextHolder.clearContext();

    Object result =
        resolver.resolveArgument(
            parameter("withAuthUser", UserPrincipal.class),
            mavContainer,
            webRequest(),
            binderFactory);

    assertTrue(result == null);
    assertNull(result);
  }

  @Test
  void resolveArgument_principal타입이다르면_null() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("string-principal", "token"));

    Object result =
        resolver.resolveArgument(
            parameter("withAuthUser", UserPrincipal.class),
            mavContainer,
            webRequest(),
            binderFactory);

    // type이 다르거나, principal 객체가 없으면
    // null 반환
    assertTrue(result == null);
  }
}
