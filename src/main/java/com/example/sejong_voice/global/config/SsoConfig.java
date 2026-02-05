package com.example.sejong_voice.global.config;


import com.example.sejong_voice.auth.dto.SsoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SsoProperties.class)
public class SsoConfig {}
