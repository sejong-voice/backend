package com.example.sejong_voice.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record SsoAuthResult(
    boolean success,
    @JsonProperty("is_auth") boolean isAuth, //
    String code,
    Map<String, Object> body // 원본 SSO 데이터
    ) {}
