package com.dongnemarket.notification.service;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.notification.dto.NotificationResponse;
import com.dongnemarket.notification.entity.Notification;
import com.dongnemarket.notification.entity.NotificationType;
import com.dongnemarket.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    /** 알림 목록 상한. 개인 목록이지만 읽은 알림이 누적되므로 최근순으로 캡한다. */
    private static final Limit MY_NOTIFICATIONS_LIMIT = Limit.of(100);

    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    public NotificationService(NotificationRepository notificationRepository, EntityManager entityManager) {
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
    }

    /**
     * 댓글 알림을 저장한다. 같은 상품의 안읽은 알림이 이미 있으면 새 row 대신 발생 시각만 갱신(코얼레싱)한다.
     * <p>동시 이벤트 레이스로 안읽은 알림이 2행 이상이면, 최신 1개만 갱신하고 나머지는 삭제해 1행으로 수렴시킨다
     * (DB 부분 유니크로 못 막는 대신 애플리케이션이 수렴). 상품 삭제·개명에도 안정적이도록 문구는 스냅샷으로 저장한다.
     */
    @Transactional
    public void notifyComment(Long recipientId, Long productId, String productTitle) {
        List<Notification> unread = notificationRepository
                .findByRecipient_IdAndProductIdAndTypeAndIsReadFalseOrderByLastNotifiedAtDesc(
                        recipientId, productId, NotificationType.COMMENT);

        if (unread.isEmpty()) {
            Member recipient = entityManager.getReference(Member.class, recipientId);
            notificationRepository.save(
                    Notification.of(recipient, NotificationType.COMMENT, buildCommentMessage(productTitle), productId));
            return;
        }

        // 최신 1개만 남겨 발생 시각을 끌어올리고(코얼레싱), 레이스로 중복된 나머지는 정리해 1행으로 수렴시킨다.
        unread.get(0).renotify();
        if (unread.size() > 1) {
            notificationRepository.deleteAll(unread.subList(1, unread.size()));
        }
    }

    /** 내 알림 목록을 최근 발생순으로 조회한다. */
    public List<NotificationResponse> getMyNotifications(Long memberId) {
        return notificationRepository
                .findByRecipient_IdOrderByLastNotifiedAtDesc(memberId, MY_NOTIFICATIONS_LIMIT)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** 내 안읽은 알림을 전부 읽음 처리한다(알림 패널 열람 시). */
    @Transactional
    public void markAllRead(Long memberId) {
        notificationRepository.markAllReadByRecipientId(memberId);
    }

    private String buildCommentMessage(String productTitle) {
        return "\"" + productTitle + "\" 글에 새로운 댓글이 작성되었습니다.";
    }
}
