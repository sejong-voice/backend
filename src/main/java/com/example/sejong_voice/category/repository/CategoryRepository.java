package com.example.sejong_voice.category.repository;

import com.example.sejong_voice.category.entity.Category;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

  List<Category> findAllByOrderByDisplayOrderAscIdAsc();
}
