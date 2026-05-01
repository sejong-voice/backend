package com.example.sejong_voice.image.exception;

import com.example.sejong_voice.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

  EMPTY_FILE(400, "IMAGE_001", "업로드할 이미지 파일이 비어 있습니다."),
  UNSUPPORTED_FILE_TYPE(400, "IMAGE_002", "지원하지 않는 이미지 형식입니다."),
  FILE_SIZE_EXCEEDED(400, "IMAGE_003", "허용 가능한 이미지 크기를 초과했습니다."),
  IMAGE_UPLOAD_FAILED(500, "IMAGE_004", "이미지 업로드에 실패했습니다."),
  IMAGE_NOT_FOUND(404, "IMAGE_005", "존재하지 않는 이미지가 포함되어 있습니다."),
  IMAGE_ACCESS_DENIED(403, "IMAGE_006", "본인이 업로드한 이미지만 사용할 수 있습니다."),
  INVALID_IMAGE_STATUS(400, "IMAGE_007", "게시글에 연결할 수 없는 이미지 상태입니다."),
  TOO_MANY_IMAGES(400, "IMAGE_008", "게시글에는 이미지를 최대 3개까지 연결할 수 있습니다."),
  INVALID_FILE_NAME(400, "IMAGE_009", "파일 이름이 유효하지 않습니다.");

  private final int status;
  private final String code;
  private final String message;
}
