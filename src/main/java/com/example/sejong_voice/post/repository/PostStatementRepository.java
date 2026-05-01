package com.example.sejong_voice.post.repository;

import com.example.sejong_voice.post.entity.PostStatement;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostStatementRepository extends JpaRepository<PostStatement, UUID> {

  long countByPostId(UUID postId);

  List<PostStatement> findAllByPostIdOrderBySequenceAsc(UUID postId);
}
