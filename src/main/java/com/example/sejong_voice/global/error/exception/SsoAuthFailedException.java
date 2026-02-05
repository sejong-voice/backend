package com.example.sejong_voice.global.error.exception;

import com.example.sejong_voice.global.error.BusinessException;
import com.example.sejong_voice.global.error.ErrorCode;

public class SsoAuthFailedException extends BusinessException {
    public SsoAuthFailedException() {
        super(ErrorCode.SSO_AUTH_FAILED);
    }
}
