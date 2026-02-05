package com.example.sejong_voice.auth;


import com.example.sejong_voice.auth.dto.SsoAuthResult;
import com.example.sejong_voice.auth.dto.SsoRequest;
import com.example.sejong_voice.auth.dto.SsoResponse;
import com.example.sejong_voice.auth.dto.SsoUser;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

public interface SchoolAuthClient {
    SsoResponse getStudentProfile(SsoRequest request);
}

