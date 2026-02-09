package com.example.sejong_voice._common.security;

import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
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
    // 1. 파라미터에 @AuthUser 어노테이션이 붙어있는지 확인
    boolean hasAnnotation = parameter.hasParameterAnnotation(AuthUser.class);

    // 2. 파라미터의 타입이 UserPrincipal 클래스인지 확인
    boolean hasType = UserPrincipal.class.isAssignableFrom(parameter.getParameterType());

    return hasAnnotation && hasType;
  }

  @Override
  public Object resolveArgument(
      @Nonnull MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      @Nonnull NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory
  ) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
      return authentication.getPrincipal();
    }
    throw new BusinessException(AuthErrorCode.INVALID_USER);
  }
}