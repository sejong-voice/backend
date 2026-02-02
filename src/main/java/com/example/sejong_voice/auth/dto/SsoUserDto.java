package com.example.sejong_voice.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsoUserDto(
        String id,
        @JsonProperty("student_id") String studentId, //
        String role,
        String name,    // 있을 때만
        String major,   // 있을 때만
        Integer year    // 있을 때만 (null 허용을 위해 Integer 사용)
){}
