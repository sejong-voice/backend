package com.example.sejong_voice.image.entity;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id")
  private Post post;

  @Column(nullable = false)
  private String originalFileName;

  @Column(nullable = false, unique = true)
  private String objectName;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private Long size;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImageStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private Integer displayOrder;

  private LocalDateTime activatedAt;

  private LocalDateTime deletedAt;

  @Builder
  public Image(
      User user,
      Post post,
      String originalFileName,
      String objectName,
      String contentType,
      Long size
  ) {
    this.user = user;
    this.post = post;
    this.originalFileName = originalFileName;
    this.objectName = objectName;
    this.contentType = contentType;
    this.size = size;
    this.status = ImageStatus.TEMP;
    this.createdAt = LocalDateTime.now();
  }

  public void activate(Post post, int displayOrder) {
    this.post = post;
    this.status = ImageStatus.ACTIVE;
    this.displayOrder = displayOrder;
    this.activatedAt = LocalDateTime.now();
    this.deletedAt = null;
  }

  public void updateDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public void markDeleted() {
    this.status = ImageStatus.DELETED;
    this.post = null;
    this.displayOrder = null;
    this.deletedAt = LocalDateTime.now();
  }
}
