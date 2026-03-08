package com.example.sejong_voice.history.repository;

import com.example.sejong_voice.history.entity.PostStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostStatusHistoryRepository extends JpaRepository<PostStatusHistory, Long> {

}
