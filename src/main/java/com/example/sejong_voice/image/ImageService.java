package com.example.sejong_voice.image;

import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.config.OciObjectStorageProperties;
import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.entity.ImageStatus;
import com.example.sejong_voice.image.entity.ImageTargetType;
import com.example.sejong_voice.image.exception.ImageErrorCode;
import com.example.sejong_voice.image.repository.ImageRepository;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Collectors;
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

  private static final int MAX_IMAGES_PER_POST = 3;
  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp"
  );

  private final ImageRepository imageRepository;
  private final OciObjectStorageProperties objectStorageProperties;
  private final Clock clock;

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
        .createdAt(Instant.now(clock))
        .build());

    log.info("TEMP 이미지 메타데이터 저장: imageId={}, userId={}", image.getId(), user.getId());
    return image;
  }

  @Transactional
  public void attachImagesToPost(User user, Post post, List<UUID> imageIds, ImageTargetType targetType) {
    if (imageIds == null || imageIds.isEmpty()) {
      return;
    }

    validateImageIds(imageIds);

    List<Image> images = imageRepository.findAllByIdIn(imageIds);
    if (images.size() != imageIds.size()) {
      throw new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND);
    }

    Map<UUID, Image> imagesById = images.stream()
        .collect(Collectors.toMap(Image::getId, image -> image));

    for (int order = 0; order < imageIds.size(); order++) {
      Image image = imagesById.get(imageIds.get(order));
      if (!image.getUser().getId().equals(user.getId())) {
        throw new BusinessException(ImageErrorCode.IMAGE_ACCESS_DENIED);
      }

      if (image.getStatus() != ImageStatus.TEMP || image.getPost() != null) {
        throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
      }

      image.activate(post, targetType, order, Instant.now(clock));
    }

    log.info("게시글 이미지 연결 완료: postId={}, imageCount={}", post.getId(), images.size());
  }

  @Transactional
  public void syncImagesForPost(User user, Post post, List<UUID> imageIds, ImageTargetType targetType) {
    List<UUID> requestedImageIds = imageIds == null ? List.of() : imageIds;

    validateImageIds(requestedImageIds);

    List<Image> currentImages = findActiveImages(post.getId(), targetType);
    Map<UUID, Image> currentImagesById = currentImages.stream()
        .collect(Collectors.toMap(Image::getId, image -> image));

    if (requestedImageIds.isEmpty()) {
      Instant now = Instant.now(clock);
      currentImages.forEach(image -> image.markDeleted(now));
      log.info("게시글 이미지 동기화 완료: postId={}, imageCount=0", post.getId());
      return;
    }

    List<Image> requestedImages = imageRepository.findAllByIdIn(requestedImageIds);
    if (requestedImages.size() != requestedImageIds.size()) {
      throw new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND);
    }

    Map<UUID, Image> requestedImagesById = requestedImages.stream()
        .collect(Collectors.toMap(Image::getId, image -> image));

    for (UUID imageId : requestedImageIds) {
      Image image = requestedImagesById.get(imageId);

      if (!image.getUser().getId().equals(user.getId())) {
        throw new BusinessException(ImageErrorCode.IMAGE_ACCESS_DENIED);
      }

      boolean isCurrentPostImage = image.getStatus() == ImageStatus.ACTIVE
          && image.getPost() != null
          && image.getPost().getId().equals(post.getId())
          && hasSameTargetType(image, targetType);
      boolean isAttachableTempImage = image.getStatus() == ImageStatus.TEMP && image.getPost() == null;

      if (!isCurrentPostImage && !isAttachableTempImage) {
        throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
      }
    }

    for (Image currentImage : currentImages) {
      if (!requestedImagesById.containsKey(currentImage.getId())) {
        currentImage.markDeleted(Instant.now(clock));
      }
    }

    for (int order = 0; order < requestedImageIds.size(); order++) {
      Image image = requestedImagesById.get(requestedImageIds.get(order));

      if (currentImagesById.containsKey(image.getId())) {
        image.updateDisplayOrder(order);
        continue;
      }

      image.activate(post, targetType, order, Instant.now(clock));
    }

    log.info("게시글 이미지 동기화 완료: postId={}, imageCount={}", post.getId(), requestedImages.size());
  }

  public List<Image> findActiveImages(UUID postId, ImageTargetType targetType) {
    if (targetType == ImageTargetType.POST_CONTENT) {
      List<Image> images = imageRepository.findAllByPostIdAndStatusAndTargetTypeIsNullOrderByDisplayOrderAsc(
          postId,
          ImageStatus.ACTIVE
      );
      images.addAll(imageRepository.findAllByPostIdAndStatusAndTargetTypeOrderByDisplayOrderAsc(
          postId,
          ImageStatus.ACTIVE,
          targetType
      ));
      images.sort(Comparator.comparing(Image::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));
      return images;
    }

    return imageRepository.findAllByPostIdAndStatusAndTargetTypeOrderByDisplayOrderAsc(
        postId,
        ImageStatus.ACTIVE,
        targetType
    );
  }

  public Map<UUID, List<Image>> findActiveImagesByPostIds(
      Collection<UUID> postIds,
      ImageTargetType targetType
  ) {
    if (postIds == null || postIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Image> images;
    if (targetType == ImageTargetType.POST_CONTENT) {
      images = imageRepository.findAllByPostIdInAndStatusAndTargetTypeIsNullOrderByDisplayOrderAsc(
          postIds,
          ImageStatus.ACTIVE
      );
      images.addAll(imageRepository.findAllByPostIdInAndStatusAndTargetTypeOrderByDisplayOrderAsc(
          postIds,
          ImageStatus.ACTIVE,
          targetType
      ));
      images.sort(Comparator.comparing(Image::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));
    } else {
      images = imageRepository.findAllByPostIdInAndStatusAndTargetTypeOrderByDisplayOrderAsc(
          postIds,
          ImageStatus.ACTIVE,
          targetType
      );
    }

    return images
        .stream()
        .collect(java.util.stream.Collectors.groupingBy(
            image -> image.getPost().getId(),
            java.util.LinkedHashMap::new,
            java.util.stream.Collectors.toList()
        ));
  }

  public List<Image> findExpiredTempImages() {
    Instant cutoff = Instant.now(clock)
        .minus(Duration.ofHours(objectStorageProperties.getTempImageRetentionHours()));
    return imageRepository.findAllByStatusAndCreatedAtBefore(ImageStatus.TEMP, cutoff);
  }

  @Transactional
  public void markDeleted(Image image) {
    image.markDeleted(Instant.now(clock));
  }

  @Transactional
  public void deleteAllByPostId(UUID postId) {
    List<Image> images = imageRepository.findAllByPostIdAndStatusOrderByDisplayOrderAsc(postId, ImageStatus.ACTIVE);
    Instant now = Instant.now(clock);
    images.forEach(image -> image.markDeleted(now));
    log.info("게시글 이미지 삭제 처리: postId={}, imageCount={}", postId, images.size());
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

  private void validateImageIds(List<UUID> imageIds) {
    if (imageIds.size() > MAX_IMAGES_PER_POST) {
      throw new BusinessException(ImageErrorCode.TOO_MANY_IMAGES);
    }

    if (imageIds.size() != Set.copyOf(imageIds).size()) {
      throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
    }
  }

  private boolean hasSameTargetType(Image image, ImageTargetType targetType) {
    if (targetType == ImageTargetType.POST_CONTENT) {
      return image.getTargetType() == null || image.getTargetType() == ImageTargetType.POST_CONTENT;
    }

    return image.getTargetType() == targetType;
  }

  private String resolveOriginalFileName(MultipartFile file) {
    String originalFileName = file.getOriginalFilename();
    if (originalFileName == null || originalFileName.isBlank()) {
      return UUID.randomUUID().toString();
    }
    return originalFileName;
  }
}
