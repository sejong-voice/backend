package com.example.sejong_voice.client;

import static org.junit.jupiter.api.Assertions.*;
import static reactor.core.publisher.Mono.when;

import com.example.sejong_voice.auth.client.HttpSchoolAuthClient;
import com.example.sejong_voice.auth.dto.*;
import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class HttpSchoolAuthClientTest {
  // SSO 요청을 하고, 이에 대한 결과(SsoResponse)를 받아
  // 유효성(res, res.user, sso.isAuth) 검증하고
  // 유효하면 반환, 아니면 SSO_AUTH_FAILED 예외 처리

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private WebClient ssoWebClient;

  private HttpSchoolAuthClient client;
  private SsoProperties ssoProperties;

  @BeforeEach
  void setUp() {
    ssoProperties = new SsoProperties();
    ssoProperties.setSessionsPath("/auth/sessions");
    ssoProperties.setTimeoutMs(1000);

    client = new HttpSchoolAuthClient(ssoWebClient, ssoProperties);
  }

  private void stubResponse(SsoRequest req, Mono<SsoResponse> mono) {
    when(ssoWebClient
            .post()
            .uri(ssoProperties.getSessionsPath())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .bodyToMono(SsoResponse.class))
        .thenReturn(mono);
  }

  private SsoUser validUser() {
    return new SsoUser("1", "20201234", "student", "홍길동", "컴퓨터공학과", 2);
  }

  @Test
  void 응답이_null이면_SSO_AUTH_FAILED() {
    SsoRequest req = new SsoRequest("20201234", "pw");

    stubResponse(req, Mono.empty()); // “SSO 응답 바디가 비어 있어서 res가 null인 상황”을 재현하는 코드입니다.

    BusinessException ex =
        assertThrows(BusinessException.class, () -> client.getStudentProfile(req));

    assertEquals(AuthErrorCode.SSO_AUTH_FAILED, ex.getErrorCode());
  }

  @Test
  void user가_null이면_SSO_AUTH_FAILED() {
    SsoRequest req = new SsoRequest("20201234", "pw");
    SsoResponse res = new SsoResponse(null, new SsoAuthResult(true, true, "OK", Map.of()));
    stubResponse(req, Mono.just(res));

    BusinessException ex =
        assertThrows(BusinessException.class, () -> client.getStudentProfile(req));

    assertEquals(AuthErrorCode.SSO_AUTH_FAILED, ex.getErrorCode());
  }

  @Test
  void sso_isAuth_false면_SSO_AUTH_FAILED() {
    SsoRequest req = new SsoRequest("20201234", "pw");
    SsoResponse res =
        new SsoResponse(validUser(), new SsoAuthResult(true, false, "FAIL", Map.of()));
    // 요청은 잘 갔는데, 응답에 body가 없고, 실패한 경우

    stubResponse(req, Mono.just(res));

    BusinessException ex =
        assertThrows(BusinessException.class, () -> client.getStudentProfile(req));

    assertEquals(AuthErrorCode.SSO_AUTH_FAILED, ex.getErrorCode());
  }

  @Test
  void 정상응답이면_그대로_반환() {
    SsoRequest req = new SsoRequest("20201234", "pw");
    SsoResponse res = new SsoResponse(validUser(), new SsoAuthResult(true, true, "OK", Map.of()));

    stubResponse(req, Mono.just(res));

    SsoResponse result = client.getStudentProfile(req);

    assertEquals(res, result);
    assertTrue(result.sso().isAuth());
  }
}
