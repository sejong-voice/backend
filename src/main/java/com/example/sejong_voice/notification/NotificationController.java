package com.example.sejong_voice.notification;

import com.example.sejong_voice._common.security.AuthUser;
import com.example.sejong_voice._common.security.UserPrincipal;
import com.example.sejong_voice.notification.dto.NotificationResponse;
import com.example.sejong_voice.notification.entity.Notification;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    //알림생성은 내부로직에서 자동으로 발생하는거라 api가 없음

    // 1. 내 알림 전체 목록
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getAll(
            @AuthUser UserPrincipal userPrincipal,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Notification> page = notificationService.getAll(userPrincipal.getUserId(), pageable);
        return ResponseEntity.ok(page.map(NotificationResponse::from));
    }

    // 2. 안 읽은 알림 목록
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnread(
            @AuthUser UserPrincipal userPrincipal,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Notification> page = notificationService.getUnread(userPrincipal.getUserId(), pageable);
        return ResponseEntity.ok(page.map(NotificationResponse::from));
    }

    // 3. 1개 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markOneAsRead(
            @PathVariable UUID notificationId,
            @AuthUser UserPrincipal userPrincipal
    ) {
        notificationService.markOneAsRead(userPrincipal.getUserId(), notificationId);
        return ResponseEntity.ok().build();
    }

    // 4. 전체 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthUser UserPrincipal userPrincipal
    ) {
        int updatedCount = notificationService.markAllAsRead(userPrincipal.getUserId());
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount));
    }
}
