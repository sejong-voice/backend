package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.SsoAuthResultDto;
import com.example.sejong_voice.auth.dto.SsoRequestDto;
import com.example.sejong_voice.auth.dto.SsoResponseDto;
import com.example.sejong_voice.auth.dto.SsoUserDto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.Map;

public interface SchoolAuthClient {
    SsoResponseDto getStudentProfile(SsoRequestDto request);
}

// 아무튼 이거 밑에 있는거 그대로 못씀, 이거 수정해야해. 데이터셋이 이상함
@Component
@Profile("dev") // 'dev' 환경에서 실행할 때만 이 가짜 로직이 작동함
class SchoolAuthClientStub implements SchoolAuthClient {
    @Override
    public SsoResponseDto getStudentProfile(SsoRequestDto request) {
        // 실제 통신 없이 가짜 데이터를 바로 리턴
        SsoUserDto user = new SsoUserDto(
                request.studentId(), // 이것도
                request.studentId(), // studentId
                "STUDENT",
                "세종이(테스트)",
                "컴퓨터공학과",
                3
        );

        SsoAuthResultDto authResult = new SsoAuthResultDto(
                true,
                true,
                "STUB_SUCCESS",
                Map.of(
                        "student_id", request.studentId(),
                        "name", "세종이(테스트)",
                        "major", "컴퓨터공학과",
                        "email", "test@sejong.ac.kr"
                )
        );

        return new SsoResponseDto(user, authResult);
    }
}
