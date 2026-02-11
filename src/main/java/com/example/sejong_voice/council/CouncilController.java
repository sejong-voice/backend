package com.example.sejong_voice.council;

import com.example.sejong_voice.council.dto.CouncilResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/councils")
@RequiredArgsConstructor
public class CouncilController {
  private final CouncilService councilService;

  @GetMapping
  public ResponseEntity<List<CouncilResponse>> getAll() {
    return ResponseEntity.ok(councilService.getAll());
  }
}
