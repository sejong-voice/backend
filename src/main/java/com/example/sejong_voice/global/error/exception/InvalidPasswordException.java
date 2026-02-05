package com.example.sejong_voice.global.error.exception;

import com.example.sejong_voice.global.error.BusinessException;
import com.example.sejong_voice.global.error.ErrorCode;


//비밀번호 불일치
public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }

}