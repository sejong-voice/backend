package com.example.sejong_voice.image;

import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.image.config.OciObjectStorageProperties;
import com.example.sejong_voice.image.dto.ImageUploadResponse;
import com.example.sejong_voice.image.dto.PostImageView;
import com.example.sejong_voice.image.dto.PostStatementImageView;
import com.example.sejong_voice.image.entity.Image;
import com.example.sejong_voice.image.entity.PostImage;
import com.example.sejong_voice.image.entity.PostStatementImage;
import com.example.sejong_voice.image.exception.ImageErrorCode;
import com.example.sejong_voice.image.repository.ImageRepository;
import com.example.sejong_voice.image.repository.PostImageRepository;
import com.example.sejong_voice.image.repository.PostStatementImageRepository;
import com.example.sejong_voice.image.storage.OciImageStorage;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostStatement;
import com.example.sejong_voice.user.entity.User;
import java.io.IOException;
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
  private final OciImageStorage ociImageStorage;
  private final OciObjectStorageProperties objectStorageProperties;
  private final Clock clock;

  @Transactional
  public ImageUploadResponse upload(User user, MultipartFile file) {
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

    if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
      throw new BusinessException(ImageErrorCode.INVALID_FILE_NAME);
    }

    String objectName = null;

    try {
      objectName = ociImageStorage.upload(user.getId(), file, file.getContentType());
      Image image = imageRepository.save(Image.builder()
          .user(user)
          .originalFileName(file.getOriginalFilename())
          .objectName(objectName)
          .contentType(file.getContentType())
          .size(file.getSize())
          .createdAt(Instant.now(clock))
          .build()
      );
      String imageUrl = ociImageStorage.getPublicUrl(objectName);

      log.info(
          "이미지 업로드 완료: imageId={}, userId={}, objectName={}",
          image.getId(), user.getId(), objectName
      );
      return ImageUploadResponse.of(image.getId(), imageUrl);
    } catch (IOException | RuntimeException e) {
      if (objectName != null) {
        try {
          ociImageStorage.delete(objectName);
        } catch (RuntimeException ex) {
          log.error("이미지 업로드 실패 후 객체 삭제 실패: objectName={}", objectName, ex);
        }
      }
      log.error("이미지 업로드 실패: userId={}", user.getId(), e);
      throw new BusinessException(ImageErrorCode.IMAGE_UPLOAD_FAILED);
    }
  }

  @Transactional
  public void attachImagesToPost(User user, Post post, List<UUID> imageIds) {
    List<Image> images = getAttachableImages(user, imageIds);
    Instant now = Instant.now(clock);

    for (int order = 0; order < images.size(); order++) {
      Image image = images.get(order);
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

  public List<PostImageView> getPostImages(UUID postId) {
    List<PostImage> postImages = postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(postId);

    return postImages.stream()
        .map(PostImage::getImage)
        .map(image -> PostImageView.of(
            image.getId(), ociImageStorage.getPublicUrl(image.getObjectName()))
        )
        .toList();
  }

  public Map<UUID, List<PostStatementImageView>> getStatementImagesByStatementIds(
      Collection<UUID> postStatementIds) {
    if (postStatementIds == null || postStatementIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return postStatementImageRepository.findAllByPostStatementIdInOrderByPostStatementIdAscDisplayOrderAsc(
            postStatementIds)
        .stream()
        .filter(mapping -> mapping.getImage().getDeletedAt() == null)
        .collect(Collectors.groupingBy(
            mapping -> mapping.getPostStatement().getId(),
            LinkedHashMap::new,
            Collectors.mapping(
                mapping -> PostStatementImageView.of(
                    mapping.getImage().getId(),
                    ociImageStorage.getPublicUrl(mapping.getImage().getObjectName())
                ),
                Collectors.toList()
            )
        ));
  }

  @Transactional
  public void cleanupExpiredTempImages() {
    Instant cutoff = Instant.now(clock)
        .minus(Duration.ofHours(objectStorageProperties.getTempImageRetentionHours()));

    List<Image> expiredImages = imageRepository.findAllExpiredTempImagesBefore(cutoff);
    for (Image image : expiredImages) {
      try {
        ociImageStorage.delete(image.getObjectName());
        image.delete(Instant.now(clock));
      } catch (RuntimeException e) {
        log.error(
            "만료된 TEMP 이미지 정리 실패: imageId={}, objectName={}",
            image.getId(), image.getObjectName(), e
        );
      }
    }
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
    postImages.forEach(image -> image.delete(now));
    statementImages.forEach(image -> image.delete(now));
    postImageRepository.deleteAllByPostId(postId);
    postStatementImageRepository.deleteAllByPostStatementPostId(postId);

    log.info("게시글 이미지 삭제 처리: postId={}, imageCount={}", postId,
        postImages.size() + statementImages.size());
  }

  private List<Image> getAttachableImages(User user, List<UUID> imageIds) {
    List<UUID> requestedImageIds = imageIds == null ? List.of() : imageIds;
    if (requestedImageIds.isEmpty()) {
      return List.of();
    }

    if (imageIds.size() > MAX_IMAGES_PER_POST) {
      throw new BusinessException(ImageErrorCode.TOO_MANY_IMAGES);
    }

    if (imageIds.size() != Set.copyOf(imageIds).size()) {
      throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
    }

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

      if (image.getDeletedAt() != null
          || postImageRepository.existsByImageId(image.getId())
          || postStatementImageRepository.existsByImageId(image.getId())) {
        throw new BusinessException(ImageErrorCode.INVALID_IMAGE_STATUS);
      }
    }
    return orderedImages;
  }
}
