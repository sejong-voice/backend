package com.example.sejong_voice.category;

import com.example.sejong_voice.category.dto.CategoryResponse;
import com.example.sejong_voice.category.entity.Category;
import com.example.sejong_voice.category.exception.CategoryErrorCode;
import com.example.sejong_voice.category.repository.CategoryRepository;
import com.example.sejong_voice.exception.BusinessException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public List<CategoryResponse> getAll() {
    return categoryRepository.findAllByOrderByDisplayOrderAscIdAsc()
        .stream()
        .map(CategoryResponse::from)
        .toList();
  }

  public Category getById(UUID categoryId) {
    return categoryRepository.findById(categoryId)
        .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));
  }
}
