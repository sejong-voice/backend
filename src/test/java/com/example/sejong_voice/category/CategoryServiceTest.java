package com.example.sejong_voice.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.sejong_voice.category.dto.CategoryResponse;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.category.exception.CategoryErrorCode;
import com.example.sejong_voice.category.repository.CategoryRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.support.TestFixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock private CategoryRepository categoryRepository;

  private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    categoryService = new CategoryService(categoryRepository);
  }

  @Test
  void getAll_정렬된_카테고리를_응답으로_변환한다() {
    Category category1 = TestFixtures.category(UUID.randomUUID(), "학사");
    Category category2 = TestFixtures.category(UUID.randomUUID(), "시설");
    when(categoryRepository.findAllByOrderByDisplayOrderAscIdAsc())
        .thenReturn(List.of(category1, category2));

    List<CategoryResponse> result = categoryService.getAll();

    assertEquals(2, result.size());
    assertEquals("학사", result.get(0).name());
    assertEquals("시설", result.get(1).name());
  }

  @Test
  void getById_없으면_CATEGORY_NOT_FOUND() {
    UUID categoryId = UUID.randomUUID();
    when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> categoryService.getById(categoryId));

    assertSame(CategoryErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
  }
}
