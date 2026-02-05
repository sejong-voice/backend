package com.example.sejong_voice.auth.client;

import com.example.sejong_voice.auth.SchoolAuthClient;
import com.example.sejong_voice.auth.dto.SsoRequest;
import com.example.sejong_voice.auth.dto.SsoResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class StubSchoolAuthClient implements SchoolAuthClient {
    @Override
    public SsoResponse getStudentProfile(SsoRequest request) {
        // 기존 스텁 로직 유지
        throw new UnsupportedOperationException("dev stub");
    }
}
