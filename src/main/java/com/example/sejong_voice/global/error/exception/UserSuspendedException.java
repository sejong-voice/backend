package com.example.sejong_voice.global.error.exception;

import com.example.sejong_voice.global.error.BusinessException;
import com.example.sejong_voice.global.error.ErrorCode;


//계정 정지
public class UserSuspendedException extends BusinessException {
    public UserSuspendedException() {
        super(ErrorCode.USER_SUSPENDED);
    }
}