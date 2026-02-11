package com.example.sejong_voice.post.repository;

import com.example.sejong_voice.post.dto.PostResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
  Page<PostResponse> search(PostSearchCondition condition, Pageable pageable);
}
