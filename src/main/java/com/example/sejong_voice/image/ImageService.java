package com.example.sejong_voice.image;

import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.config.OciObjectStorageProperties;
import com.example.sejong_voice.image.dto.PostImageResponse;
import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.entity.ImageStatus;
import com.example.sejong_voice.image.exception.ImageErrorCode;
import com.example.sejong_voice.image.repository.ImageRepository;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp"
  );

  private final ImageRepository imageRepository;
  private final OciObjectStorageProperties objectStorageProperties;
  private final com.example.sejong_voice.image.storage.OciImageStorage ociImageStorage;

  public void validateUploadFile(MultipartFile file) {
    validate(file);
  }

  @Transactional
  public Image createTempImage(User user, MultipartFile file, String objectName) {
    Image image = imageRepository.save(Image.builder()
        .user(user)
        .originalFileName(resolveOriginalFileName(file))
        .objectName(objectName)
        .contentType(file.getContentType())
        .size(file.getSize())
        .build());

    log.info("TEMP 이미지 메타데이터 저장: imageId={}, userId={}", image.getId(), user.getId());
    return image;
  }

  @Transactional
  public void attachImagesToPost(User user, Post post, List<UUID> imageIds) {
    if (imageIds == null || imageIds.isEmpty()) {
      return;
    }

    List<Image> images = imageRepository.findAllByIdIn(imageIds);
    if (images.size() != imageIds.size()) {
      throw new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND);
    }

    for (Image image : images) {
      if (!image.getUser().getId().equals(user.getId())) {
        throw new BusinessException(ImageErrorCode.IMAGE_ACCESS_DENIED);
      }

      if (image.getStatus() != ImageStatus.TEMP || image.getPost() != null) {
        throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
      }

      image.activate(post);
    }

    log.info("게시글 이미지 연결 완료: postId={}, imageCount={}", post.getId(), images.size());
  }

  public List<PostImageResponse> getActiveImages(UUID postId) {
    return imageRepository.findAllByPostIdAndStatusOrderByCreatedAtAsc(postId, ImageStatus.ACTIVE)
        .stream()
        .map(this::toPostImageResponse)
        .toList();
  }

  public Map<UUID, List<PostImageResponse>> getActiveImagesByPostIds(Collection<UUID> postIds) {
    if (postIds == null || postIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return imageRepository.findAllByPostIdInAndStatusOrderByCreatedAtAsc(postIds, ImageStatus.ACTIVE)
        .stream()
        .collect(java.util.stream.Collectors.groupingBy(
            image -> image.getPost().getId(),
            java.util.LinkedHashMap::new,
            java.util.stream.Collectors.mapping(this::toPostImageResponse, java.util.stream.Collectors.toList())
        ));
  }

  public List<Image> findExpiredTempImages() {
    LocalDateTime cutoff = LocalDateTime.now()
        .minusHours(objectStorageProperties.getTempImageRetentionHours());
    return imageRepository.findAllByStatusAndCreatedAtBefore(ImageStatus.TEMP, cutoff);
  }

  @Transactional
  public void markDeleted(Image image) {
    image.markDeleted();
  }

  private PostImageResponse toPostImageResponse(Image image) {
    return new PostImageResponse(image.getId(), ociImageStorage.createReadAccessUrl(image.getObjectName()));
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ImageErrorCode.EMPTY_FILE);
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    if (file.getSize() > objectStorageProperties.getMaxFileSizeBytes()) {
      throw new BusinessException(ImageErrorCode.FILE_SIZE_EXCEEDED);
    }
  }

  private String resolveOriginalFileName(MultipartFile file) {
    String originalFileName = file.getOriginalFilename();
    if (originalFileName == null || originalFileName.isBlank()) {
      return UUID.randomUUID().toString();
    }
    return originalFileName;
  }
}
