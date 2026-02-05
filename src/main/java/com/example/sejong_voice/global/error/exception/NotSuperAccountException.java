package com.example.sejong_voice.global.error.exception;


import com.example.sejong_voice.global.error.BusinessException;
import com.example.sejong_voice.global.error.ErrorCode;


//관리자 권한 없음
public class NotSuperAccountException extends BusinessException {
    public NotSuperAccountException() {
        super(ErrorCode.NOT_SUPER_ACCOUNT);
    }
}
