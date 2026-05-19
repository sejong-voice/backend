package com.example.sejong_voice.exception;

import com.example.sejong_voice._common.ErrorResponse;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
  private final Clock clock;

  /** 비즈니스 로직 예외 (BusinessException) - ErrorCode 인터페이스를 통해 AuthErrorCode, UserErrorCode 등을 모두 처리 */
  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();

    log.warn(
        "[Business Exception] Code: {}, Message: {}", errorCode.getCode(), errorCode.getMessage());

    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, clock));
  }

  /** HTTP 메서드가 지원되지 않는 경우 예외 */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
      HttpRequestMethodNotSupportedException e) {
    log.warn("[Method Not Allowed] {}", e.getMessage());

    GlobalErrorCode errorCode = GlobalErrorCode.METHOD_NOT_ALLOWED;

    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, clock));
  }

  /** Valid 유효성 검사 실패 예외 - DTO의 @NotNull, @Email 등 검증 실패 시 발생 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e) {
    log.warn("[Validation Exception] {}", e.getMessage());

    GlobalErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;

    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, clock));
  }

  /** JSON 파싱 실패 예외 - 요청 바디가 JSON 형식이 아니거나, 타입이 안 맞을 때 발생 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleJsonException(HttpMessageNotReadableException e) {
    log.warn("[JSON Parse Error] {}", e.getMessage());

    GlobalErrorCode errorCode = GlobalErrorCode.JSON_PARSE_ERROR;

    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, clock));
  }

  /** multipart 업로드 크기 제한 초과 예외 */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException e) {
    log.warn("[Max Upload Size Exceeded] {}", e.getMessage());

    GlobalErrorCode errorCode = GlobalErrorCode.MAX_UPLOAD_SIZE_EXCEEDED;

    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, clock));
  }

  /** 시스템 예외 - 위에서 잡지 못한 모든 예외 (NullPointer, DB 연결 실패 등) */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
    log.error("[ServerError] Unexpected Exception occurred: ", ex);

    GlobalErrorCode errorCode = GlobalErrorCode.UNKNOWN_ERROR;

    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, clock));
  }
}
