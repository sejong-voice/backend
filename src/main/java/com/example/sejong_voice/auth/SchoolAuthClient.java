package com.example.sejong_voice.auth;

import com.example.sejong_voice.auth.dto.SsoRequest;
import com.example.sejong_voice.auth.dto.SsoResponse;

public interface SchoolAuthClient {
  SsoResponse getStudentProfile(SsoRequest request);
}
