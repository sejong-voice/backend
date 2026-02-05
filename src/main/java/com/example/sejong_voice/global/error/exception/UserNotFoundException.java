package com.example.sejong_voice.global.error.exception;


import com.example.sejong_voice.global.error.BusinessException;
import com.example.sejong_voice.global.error.ErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}