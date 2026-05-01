package com.example.sejong_voice.image;

import com.example.sejong_voice.image.dto.ImageUploadResponse;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageFacade {

  private final ImageService imageService;
  private final UserService userService;

  @Transactional
  public ImageUploadResponse upload(UUID userId, MultipartFile file) {
    User user = userService.getById(userId);
    return imageService.upload(user, file);
  }

  @Transactional
  public void cleanupExpiredTempImages() {
    imageService.cleanupExpiredTempImages();
  }
}
