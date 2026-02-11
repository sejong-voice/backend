package com.example.sejong_voice.council;

import com.example.sejong_voice.council.dto.CouncilResponse;
import com.example.sejong_voice.council.repository.CouncilRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouncilService {
  private final CouncilRepository councilRepository;

  public List<CouncilResponse> getAll() {
    return councilRepository.findAll().stream()
        .map(CouncilResponse::new)
        .collect(Collectors.toList());
  }
}
