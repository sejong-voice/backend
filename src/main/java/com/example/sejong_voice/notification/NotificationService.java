package com.example.sejong_voice.notification;

import com.example.sejong_voice.council.exception.CouncilErrorCode;
import com.example.sejong_voice.council.repository.CouncilMemberRepository;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.notification.entity.Notification;
import com.example.sejong_voice.notification.exception.NotificationErrorCode;
import com.example.sejong_voice.notification.repository.NotificationRepository;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.repository.PostRepository;
import com.example.sejong_voice.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.example.sejong_voice.notification.entity.NotificationType.POST_APPROVED;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final PostRepository postRepository;

    //createPostApprovedNotifications , notification 테이블에 상태전환 로그 남기는 메서드
    @Transactional
    public void createPostApprovedNotifications(Post post) {
        // 1. post검증은 이미 완료됨
        // 2. 학생회 계정 : post.council -> council.id -> council_member.user(councilId로 조회)
        UUID councilId = post.getCouncil().getId();
        User councilUser = councilMemberRepository.findUserByCouncilId(councilId)
                // post의 councilId로 councilMembers에서 조회하는데 해당 학생회가 없다면 council_not_found
                // 학생회가 있다면, 해당 학생회 계정으로 매핑된 user(학생회계정)를 가져옴
                .orElseThrow(() -> new BusinessException(CouncilErrorCode.COUNCIL_NOT_FOUND));
        //?????? council_member에 학생회별 계정 만드는 로직이 있음??
        //councilService에 없던데?????

        Notification notification = Notification.builder()
                .user(councilUser)
                .type(POST_APPROVED)
                .post(post)
                .build();

        notificationRepository.save(notification);
    }

    // 1. 내 알림 전체 목록 조회
    public Page<Notification> getAll(UUID userId, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }

    // 2. 안 읽은 알림 조회
    public Page<Notification> getUnread(UUID userId, Pageable pageable) {
        return notificationRepository.findByUser_IdAndReadTFIsFalseOrderByCreatedAtDesc(userId, pageable);
    }

    // 3. 1개 알림 읽음 처리
    @Transactional
    public void markOneAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead(); // readTF = true
    }

    // 4. 전체 알림 읽음 처리
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId);
    }

}
