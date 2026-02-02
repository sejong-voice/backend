package com.example.sejong_voice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // 프로젝트 내의 모든 properties클래스를 자동으로 탐색
public class SejongVoiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(SejongVoiceApplication.class, args);
	}

}

// git 기본 사용법 메뉴얼로 정리해둬야할듯