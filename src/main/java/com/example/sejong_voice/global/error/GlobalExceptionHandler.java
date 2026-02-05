package com.example.sejong_voice.global.error;


import com.example.sejong_voice.auth.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.security.sasl.AuthenticationException;
import java.rmi.AccessException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {// 모든 컨트롤러의 예외를 여기서 관리

    //1.권한 부족(super 전용 기능에 일반 유저가 접근했을 때)
    @ExceptionHandler(AccessException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "AUTH_003",//커스텀 에러 코드
                "해당 기능에 대한 접근 권한이 없습니다. *(super계정 전용)",
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }
    //2. 로그인 실패 / 인증 관련 예외
    @ExceptionHandler(AuthenticationException.class)//커스텀 예외를 만들면 좋다고 하네요...
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "AUTH_001",
                "학번 또는 비밀번호가 일치하지 않습니다.",
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(BusinessException.class) //커스텀 에러 처리
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, WebRequest request) {
        ErrorCode errorCode = e.getErrorCode();

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus(),
                errorCode.name(),
                errorCode.getCode(),
                errorCode.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }

    //3. 그 외 모든 예상치 못한 서버 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "COMMON_001",
                "서버 내부 오류가 발생했습니다.",
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}



/*
 예외처리를 커스텀해서 처리하고 이에 대한 처리는 공통적으로 globalExceptionHandler에서 받아서 처리
 이로서 내부로직에 대한 예외처리는 모두 globalExceptionHandler를 거쳐서 갈 수 있게끔
 커스텀 에러는 ErrorResponse를 통해 globalExceptionHandler로 전달됨
 */