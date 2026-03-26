package com.example.sejong_voice.post.repository;

import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
  Page<PostListResponse> search(PostSearchCondition condition, Pageable pageable);
}
