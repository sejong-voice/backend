package com.example.sejong_voice.image;

import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.config.OciObjectStorageProperties;
import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.entity.ImageStatus;
import com.example.sejong_voice.image.entity.PostImage;
import com.example.sejong_voice.image.entity.PostStatementImage;
import com.example.sejong_voice.image.exception.ImageErrorCode;
import com.example.sejong_voice.image.repository.ImageRepository;
import com.example.sejong_voice.image.repository.PostImageRepository;
import com.example.sejong_voice.image.repository.PostStatementImageRepository;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.user.entity.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
  private final PostImageRepository postImageRepository;
  private final PostStatementImageRepository postStatementImageRepository;
  private final OciObjectStorageProperties objectStorageProperties;
  private final Clock clock;

  public void validateUploadFile(MultipartFile file) {
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
  public void attachImagesToPost(User user, Post post, List<UUID> imageIds) {
    List<Image> images = getAttachableImages(user, imageIds);
    Instant now = Instant.now(clock);

    for (int order = 0; order < images.size(); order++) {
      Image image = images.get(order);
      image.activate(now);
      postImageRepository.save(
          PostImage.builder()
              .post(post)
              .image(image)
              .displayOrder(order)
              .createdAt(now)
              .build()
      );
    }
    log.info("게시글 이미지 연결 완료: postId={}, imageCount={}", post.getId(), images.size());
  }

  @Transactional
  public void attachImagesToStatement(User user, PostStatement postStatement, List<UUID> imageIds) {
    List<Image> images = getAttachableImages(user, imageIds);
    Instant now = Instant.now(clock);

    for (int order = 0; order < images.size(); order++) {
      Image image = images.get(order);
      image.activate(now);
      postStatementImageRepository.save(
          PostStatementImage.builder()
              .postStatement(postStatement)
              .image(image)
              .displayOrder(order)
              .createdAt(now)
              .build()
      );
    }
    log.info("입장문 이미지 연결 완료: statementId={}, imageCount={}", postStatement.getId(), images.size());
  }

  public List<Image> getPostImages(UUID postId) {
    return postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(postId).stream()
        .map(PostImage::getImage)
        .filter(image -> image.getStatus() == ImageStatus.ACTIVE)
        .toList();
  }

  public Map<UUID, List<Image>> getPostImagesByPostIds(Collection<UUID> postIds) {
    if (postIds == null || postIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return postImageRepository.findAllByPostIdInOrderByDisplayOrderAsc(postIds).stream()
        .filter(mapping -> mapping.getImage().getStatus() == ImageStatus.ACTIVE)
        .collect(Collectors.groupingBy(
            mapping -> mapping.getPost().getId(),
            LinkedHashMap::new,
            Collectors.mapping(PostImage::getImage, Collectors.toList())
        ));
  }

  public Map<UUID, List<Image>> getStatementImagesByStatementIds(
      Collection<UUID> postStatementIds) {
    if (postStatementIds == null || postStatementIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return postStatementImageRepository.findAllByPostStatementIdInOrderByPostStatementIdAscDisplayOrderAsc(
            postStatementIds)
        .stream()
        .filter(mapping -> mapping.getImage().getStatus() == ImageStatus.ACTIVE)
        .collect(Collectors.groupingBy(
            mapping -> mapping.getPostStatement().getId(),
            LinkedHashMap::new,
            Collectors.mapping(PostStatementImage::getImage, Collectors.toList())
        ));
  }

  public List<Image> getExpiredTempImages() {
    Instant cutoff = Instant.now(clock)
        .minus(Duration.ofHours(objectStorageProperties.getTempImageRetentionHours()));
    return imageRepository.findAllByStatusAndCreatedAtBefore(ImageStatus.TEMP, cutoff);
  }

  @Transactional
  public void deleteImage(Image image) {
    image.markDeleted(Instant.now(clock));
  }

  @Transactional
  public void deleteAllByPostId(UUID postId) {
    List<Image> postImages = postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(postId)
        .stream()
        .map(PostImage::getImage)
        .toList();
    List<Image> statementImages = postStatementImageRepository.findAllByPostStatementPostId(postId)
        .stream()
        .map(PostStatementImage::getImage)
        .toList();

    Instant now = Instant.now(clock);
    postImages.forEach(image -> image.markDeleted(now));
    statementImages.forEach(image -> image.markDeleted(now));
    postImageRepository.deleteAllByPostId(postId);
    postStatementImageRepository.deleteAllByPostStatementPostId(postId);

    log.info("게시글 이미지 삭제 처리: postId={}, imageCount={}", postId,
        postImages.size() + statementImages.size());
  }

  private void validateImageIds(List<UUID> imageIds) {
    if (imageIds.size() > MAX_IMAGES_PER_POST) {
      throw new BusinessException(ImageErrorCode.TOO_MANY_IMAGES);
    }

    if (imageIds.size() != Set.copyOf(imageIds).size()) {
      throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
    }
  }

  private List<Image> getAttachableImages(User user, List<UUID> imageIds) {
    List<UUID> requestedImageIds = imageIds == null ? List.of() : imageIds;
    if (requestedImageIds.isEmpty()) {
      return List.of();
    }

    validateImageIds(requestedImageIds);

    List<Image> images = imageRepository.findAllByIdIn(requestedImageIds);
    if (images.size() != requestedImageIds.size()) {
      throw new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND);
    }

    Map<UUID, Image> imagesById = images.stream()
        .collect(Collectors.toMap(Image::getId, image -> image));

    List<Image> orderedImages = requestedImageIds.stream()
        .map(imagesById::get)
        .toList();

    for (Image image : orderedImages) {
      if (!image.getUser().getId().equals(user.getId())) {
        throw new BusinessException(ImageErrorCode.IMAGE_ACCESS_DENIED);
      }

      if (image.getStatus() != ImageStatus.TEMP) {
        throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
      }
    }

    return orderedImages;
  }

  private String resolveOriginalFileName(MultipartFile file) {
    String originalFileName = file.getOriginalFilename();
    if (originalFileName == null || originalFileName.isBlank()) {
      return UUID.randomUUID().toString();
    }
    return originalFileName;
  }
}
