package com.example.sejong_voice.auth.client;


import com.example.sejong_voice.auth.SchoolAuthClient;
import com.example.sejong_voice.auth.dto.SsoProperties;
import com.example.sejong_voice.auth.dto.SsoRequest;
import com.example.sejong_voice.auth.dto.SsoResponse;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Profile("local") // local에서만 실서버 호출
@RequiredArgsConstructor
public class HttpSchoolAuthClient implements SchoolAuthClient {

  private final WebClient ssoWebClient;
  private final SsoProperties ssoProperties;

  @Override
  public SsoResponse getStudentProfile(SsoRequest request) {

    SsoResponse res = ssoWebClient.post()
        .uri(ssoProperties.getSessionsPath())          // "/auth/sessions"
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)                            // ✅ SsoRequest가 student_id로 직렬화됨
        .retrieve()
        .bodyToMono(SsoResponse.class)                 // ✅ 너 DTO로 역직렬화
        .block(Duration.ofMillis(ssoProperties.getTimeoutMs()));

    // 최소 유효성 체크 (원하는 정책대로 강화 가능)
    if (res == null || res.user() == null) {
      throw new BusinessException(AuthErrorCode.SSO_AUTH_FAILED);
    }
    // sso 블록이 반드시 온다는 보장이 있다면 이것도 체크 가능
    if (res.sso() != null && !res.sso().isAuth()) {
      throw new BusinessException(AuthErrorCode.SSO_AUTH_FAILED);
    }

    return res;
  }
}
