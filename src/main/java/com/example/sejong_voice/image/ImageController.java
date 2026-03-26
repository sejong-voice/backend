package com.example.sejong_voice.image;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.image.dto.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "이미지 API", description = "게시글 이미지 업로드 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

  private final ImageFacade imageFacade;

  @Operation(
      summary = "이미지 업로드",
      description = "게시글 작성 전에 이미지를 업로드하고 TEMP 상태로 저장합니다."
  )
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageUploadResponse> upload(
      @RequestPart("image") MultipartFile image,
      @AuthUser UserPrincipal userPrincipal
  ) {
    ImageUploadResponse response = imageFacade.upload(userPrincipal.getUserId(), image);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
