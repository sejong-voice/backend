package com.example.sejong_voice.category;

import com.example.sejong_voice.category.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "카테고리 API", description = "게시글 카테고리 조회 API")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  @Operation(summary = "카테고리 목록 조회", description = "게시글 작성 및 조회에 사용하는 전체 카테고리 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<List<CategoryResponse>> getAll() {
    return ResponseEntity.ok(categoryService.getAll());
  }
}
