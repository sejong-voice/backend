package com.example.sejong_voice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class SejongVoiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SejongVoiceApplication.class, args);
  }
}