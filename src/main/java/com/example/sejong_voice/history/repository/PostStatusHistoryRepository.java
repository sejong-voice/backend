package com.example.sejong_voice.history.repository;

import com.example.sejong_voice.history.entity.PostStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostStatusHistoryRepository extends JpaRepository<PostStatusHistory, UUID> {}
