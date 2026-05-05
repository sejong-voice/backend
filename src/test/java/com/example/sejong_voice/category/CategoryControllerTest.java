package com.example.sejong_voice.category;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sejong_voice.category.dto.CategoryResponse;
import com.example.sejong_voice.exception.GlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

  @Mock private CategoryService categoryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new CategoryController(categoryService))
            .setControllerAdvice(new GlobalExceptionHandler(fixedClock))
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();
  }

  @Test
  void getAll_카테고리목록을_반환한다() throws Exception {
    when(categoryService.getAll())
        .thenReturn(
            List.of(
                new CategoryResponse(UUID.randomUUID(), "학사"),
                new CategoryResponse(UUID.randomUUID(), "시설")));

    mockMvc
        .perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("학사"))
        .andExpect(jsonPath("$[1].name").value("시설"));

    verify(categoryService).getAll();
  }
}
