package com.example.sejong_voice.notification.repository;

import com.example.sejong_voice.notification.entity.Notification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Page<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  Page<Notification> findByUser_IdAndReadTFIsFalseOrderByCreatedAtDesc(
      UUID userId, Pageable pageable);

  Optional<Notification> findByIdAndUser_Id(UUID notificationId, UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Notification n
         set n.readTF = true
       where n.user.id = :userId
         and n.readTF = false
      """)
  int markAllAsRead(@Param("userId") UUID userId);
}
