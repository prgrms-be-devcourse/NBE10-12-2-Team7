package com.dongnemarket.notification.repository;

import com.dongnemarket.notification.entity.Notification;
import com.dongnemarket.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 코얼레싱 대상 조회: 같은 수신자·상품·타입의 <em>안읽은</em> 알림을 찾는다.
     * {@code (recipient_id, product_id, type, is_read=false)}는 최대 1행이라는 코얼레싱 불변식에 대응한다.
     * 있으면 {@code renotify}, 없으면 새로 저장하는 것이 핸들러(PR-N1b)의 분기다.
     */
    Optional<Notification> findByRecipient_IdAndProductIdAndTypeAndIsReadFalse(
            Long recipientId, Long productId, NotificationType type);

    /** 내 알림 목록을 최근 발생순으로 조회한다(개인 목록이라 바운드가 작다). */
    List<Notification> findByRecipient_IdOrderByLastNotifiedAtDesc(Long recipientId);
}
