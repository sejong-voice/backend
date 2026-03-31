package com.example.sejong_voice.image;

import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.dto.ImageUploadResponse;
import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.exception.ImageErrorCode;
import com.example.sejong_voice.image.storage.OciImageStorage;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.user.entity.User;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
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
  private final OciImageStorage ociImageStorage;

  @Transactional
  public ImageUploadResponse upload(UUID userId, MultipartFile file) {
    imageService.validateUploadFile(file);

    User user = userService.getById(userId);
    String objectName = null;

    try {
      objectName = ociImageStorage.upload(userId, file, file.getContentType());
      Image image = imageService.createTempImage(user, file, objectName);
      String imageUrl = ociImageStorage.getPublicUrl(objectName);

      log.info("이미지 업로드 완료: imageId={}, userId={}, objectName={}", image.getId(), userId, objectName);
      return new ImageUploadResponse(image.getId(), imageUrl);
    } catch (IOException | RuntimeException e) {
      cleanupUploadedObject(objectName);
      log.error("이미지 업로드 실패: userId={}", userId, e);
      throw new BusinessException(ImageErrorCode.IMAGE_UPLOAD_FAILED);
    }
  }

  private void cleanupUploadedObject(String objectName) {
    if (objectName == null) {
      return;
    }

    try {
      ociImageStorage.delete(objectName);
    } catch (RuntimeException e) {
      log.error("업로드 실패 후 Object Storage 정리 실패: objectName={}", objectName, e);
    }
  }

  @Transactional
  public void cleanupExpiredTempImages() {
    for (Image image : imageService.findExpiredTempImages()) {
      try {
        ociImageStorage.delete(image.getObjectName());
        imageService.markDeleted(image);
      } catch (RuntimeException e) {
        log.error("만료된 TEMP 이미지 정리 실패: imageId={}, objectName={}", image.getId(),
            image.getObjectName(), e);
      }
    }
  }

  @Transactional(readOnly = true)
  public List<PostImageResponse> getActiveImages(UUID postId) {
    return imageService.findActiveImages(postId).stream()
        .map(this::toPostImageResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<UUID, List<PostImageResponse>> getActiveImagesByPostIds(Collection<UUID> postIds) {
    return imageService.findActiveImagesByPostIds(postIds).entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .map(this::toPostImageResponse)
                .toList(),
            (left, right) -> left,
            LinkedHashMap::new
        ));
  }

  private PostImageResponse toPostImageResponse(Image image) {
    return new PostImageResponse(
        image.getId(),
        ociImageStorage.getPublicUrl(image.getObjectName())
    );
  }
}
