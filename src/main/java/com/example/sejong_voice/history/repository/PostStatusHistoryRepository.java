package com.example.sejong_voice.history.repository;

import com.example.sejong_voice.history.entity.PostStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostStatusHistoryRepository extends JpaRepository<PostStatusHistory, UUID> {

}
