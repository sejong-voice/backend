package com.example.sejong_voice.notification.entity;


import com.example.sejong_voice.notification.entity.NotificationType;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id; //알림 로그 id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    // 알림에 해당하는 user
    // 내 알림 조회에서, 해당 user-> userId로 타고 들어가서 토큰의 userid와 일치하는지 검사

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type; //어떤 알림 종류인지

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post; //어떤 게시글의 알림인지

    @Column(name = "read_tf", nullable = false)
    private Boolean readTF;// 읽음 유무


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(User user, NotificationType type, Post post) {
        this.id = UUID.randomUUID();
        this.user = user; // 알림 받을 유저
        this.type = type; // 어떤 알림인지
        this.post = post; // 어디 게시글에서 온 알림인지
        this.readTF = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {this.readTF = true;}//읽음으로 전환하는 로직

    public boolean isRead() {return this.readTF;}//읽음 유무 반환 로직
}