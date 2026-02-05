package com.example.sejong_voice.global.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 부모인 RuntimeException에 메시지 전달
        this.errorCode = errorCode;
    }
}