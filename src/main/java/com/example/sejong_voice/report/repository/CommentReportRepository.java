package com.example.sejong_voice.report.repository;

import com.example.sejong_voice.report.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {
    boolean existsByCommentIdAndUserId(UUID commentId, UUID reporterId);

    long countByComment_User_Id(UUID targetUserId);
    // long 그대로 반환해도 됩니다. (보통 count는 long 권장)
}
