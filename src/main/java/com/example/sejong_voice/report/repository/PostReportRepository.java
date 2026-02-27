package com.example.sejong_voice.report.repository;

import com.example.sejong_voice.report.entity.PostReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface PostReportRepository extends JpaRepository<PostReport, UUID> {

    boolean existsByPostIdAndReporterUserId(UUID postId, UUID reporterId);

    long countByPost_Id(UUID postId);
}
