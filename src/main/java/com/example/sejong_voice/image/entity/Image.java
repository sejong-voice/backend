package com.example.sejong_voice.image.entity;

import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
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

  @Column(nullable = false)
  private String originalFileName;

  @Column(nullable = false, unique = true)
  private String objectName;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private Long size;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant deletedAt;

  @Builder
  public Image(
      User user,
      String originalFileName,
      String objectName,
      String contentType,
      Long size,
      Instant createdAt) {
    this.user = user;
    this.originalFileName = originalFileName;
    this.objectName = objectName;
    this.contentType = contentType;
    this.size = size;
    this.createdAt = createdAt;
  }

  public void delete(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }
}
