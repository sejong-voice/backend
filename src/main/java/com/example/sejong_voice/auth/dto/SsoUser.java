package com.example.sejong_voice.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsoUser(
        String id,                          // @NotBlank 적용 가능(Python 응답 보장 시)
        @JsonProperty("student_id") String studentNo, // @NotBlank 적용 가능(필수 학번)
        String role,                        // @NotBlank 적용 가능(권한 필수 시)
        String name,                        // 있을 때만
        @JsonProperty("major") String department,// 있을 때만
        Integer year                        // 있을 때만 (null 허용을 위해 Integer 사용)

){}

/*
name, department, year는 코드 주석대로 선택적 필드라 @NotNull/@NotBlank를 걸면
 SSO 응답에 값이 비어 있을 때 바로 검증 예외가 납니다.
 계약을 바꿔 항상 내려준다고 확정하지 않는 한 붙이지 않는 게 맞습니다.
- 이 DTO는 외부(Python SSO) 응답을 역직렬화할 때 쓰이는 타입이라,
실제 검증을 돌리려면 해당 파라미터에 @Valid를 붙이고 Bean Validation을 호출해야 합니다.
그렇지 않으면 어노테이션을 달아도 동작하지 않습니다.

 */