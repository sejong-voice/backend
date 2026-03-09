// NotificationResponse.java
package com.example.sejong_voice.notification.dto;

import com.example.sejong_voice.notification.entity.Notification;
import com.example.sejong_voice.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        UUID postId,
        NotificationType type,
        boolean readTF,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUser().getId(),
                n.getPost().getId(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
