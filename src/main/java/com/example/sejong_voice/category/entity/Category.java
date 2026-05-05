package com.example.sejong_voice.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private Integer displayOrder;

  @Builder
  public Category(String name, Integer displayOrder) {
    this.name = name;
    this.displayOrder = displayOrder;
  }
}
