package com.example.sejong_voice.auth.dto;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "sso")
public class SsoProperties {
    private String baseUrl;
    private String sessionsPath;
    private int timeoutMs;
}
