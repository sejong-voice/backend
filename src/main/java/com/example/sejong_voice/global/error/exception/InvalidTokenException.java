package com.example.sejong_voice.global.error.exception;


import com.example.sejong_voice.global.error.BusinessException;
import com.example.sejong_voice.global.error.ErrorCode;


//토큰 관련 에러
public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(ErrorCode errorCode) {
        super(errorCode);
    }

}