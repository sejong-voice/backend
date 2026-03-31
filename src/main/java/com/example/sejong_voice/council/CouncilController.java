package com.example.sejong_voice.council;

import com.example.sejong_voice.council.dto.CouncilResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학생회 API", description = "학생회 목록 조회 API")
@RestController
@RequestMapping("/api/v1/councils")
@RequiredArgsConstructor
public class CouncilController {
  private final CouncilService councilService;

  @Operation(
      summary = "학생회 목록 조회",
      description = "전체 학생회 목록을 조회합니다. keyword를 전달하면 학생회 이름 기준으로 검색합니다."
  )
  @GetMapping
  public ResponseEntity<List<CouncilResponse>> getAll(
      @RequestParam(required = false) String keyword
  ) {
    return ResponseEntity.ok(councilService.getAll(keyword));
  }
}
