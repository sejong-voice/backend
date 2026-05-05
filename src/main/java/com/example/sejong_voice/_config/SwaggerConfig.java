package com.example.sejong_voice._config;

import com.example.sejong_voice._common.security.AuthUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @PostConstruct
  public void init() {
    // @AuthUser 어노테이션이 붙은 파라미터를 스웨거 문서에서 완전히 제외
    SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthUser.class);
  }

  @Bean
  public OpenAPI openAPI() {
    String jwtSchemeName = "jwtAuth";

    // API 요청 시 JWT 토큰을 헤더에 추가하도록 설정
    SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

    // SecurityScheme 정의
    Components components =
        new Components()
            .addSecuritySchemes(
                jwtSchemeName,
                new SecurityScheme()
                    .name(jwtSchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("Bearer")
                    .bearerFormat("JWT"));

    return new OpenAPI()
        .info(
            new Info()
                .title("Sejong Voice API 문서")
                .description("세종 신문고 프로젝트 API 명세서입니다.")
                .version("v1.0.0"))
        .addSecurityItem(securityRequirement)
        .components(components);
  }
}
