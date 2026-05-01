package com.example.sejong_voice._common.security;

import jakarta.annotation.Nonnull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class UserPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    boolean hasAnnotation = parameter.hasParameterAnnotation(AuthUser.class);

    boolean hasType = UserPrincipal.class.isAssignableFrom(parameter.getParameterType());

    return hasAnnotation && hasType;
  }

  @Override
  public Object resolveArgument(
      @Nonnull MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      @Nonnull NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
      return authentication.getPrincipal();
    }
    return null;
  }
}
