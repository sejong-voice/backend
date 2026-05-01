package com.example.sejong_voice.report.repository;

import com.example.sejong_voice.report.entity.PostReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportRepository extends JpaRepository<PostReport, UUID> {

  boolean existsByPostIdAndReporterUserId(UUID postId, UUID reporterId);

  long countByPost_Id(UUID postId);
}
