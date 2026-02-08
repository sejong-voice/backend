package com.example.sejong_voice._config;

import com.example.sejong_voice.auth.dto.SsoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final SsoProperties ssoProperties;


    @Bean
    public WebClient ssoWebClient() {
        return WebClient.builder()
                .baseUrl(ssoProperties.getBaseUrl())
                .build();
    }
}
