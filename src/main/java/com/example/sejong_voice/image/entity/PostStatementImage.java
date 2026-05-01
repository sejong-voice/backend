package com.example.sejong_voice.image.entity;

import com.example.sejong_voice.post.entity.PostStatement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_post_statement_image_image",
          columnNames = {"image_id"}),
      @UniqueConstraint(
          name = "uk_post_statement_image_statement_display_order",
          columnNames = {"post_statement_id", "display_order"})
    })
public class PostStatementImage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_statement_id", nullable = false)
  private PostStatement postStatement;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "image_id", nullable = false)
  private Image image;

  @Column(nullable = false)
  private Integer displayOrder;

  @Column(nullable = false)
  private Instant createdAt;

  @Builder
  public PostStatementImage(
      PostStatement postStatement, Image image, Integer displayOrder, Instant createdAt) {
    this.postStatement = postStatement;
    this.image = image;
    this.displayOrder = displayOrder;
    this.createdAt = createdAt;
  }
}
