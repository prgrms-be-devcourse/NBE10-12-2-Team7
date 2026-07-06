package com.dongnemarket.notification.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 저장형 알림 한 건(현재 댓글 알림). 수신자에게 pull 방식으로 내려준다.
 * <p><b>상품별 코얼레싱</b>: 같은 수신자·상품·타입의 <em>안읽은</em> 알림은 최대 1행만 유지한다.
 * 같은 상품에 새 이벤트가 또 오면 새 row를 만들지 않고 {@link #renotify()}로 {@code lastNotifiedAt}만
 * 갱신해 목록 맨 위로 끌어올린다. 사용자가 읽으면({@link #markRead()}) 다음 이벤트가 새 알림이 된다.
 * <p><b>productId는 연관이 아닌 id 스냅샷</b>이다. 상품이 삭제·개명돼도 알림이 보존되고,
 * 목록 렌더링에 필요한 문구를 {@code message}에 스냅샷으로 담아 N개 렌더 시 조인이 없다
 * (채팅방의 seller 스냅샷과 같은 철학).
 */
@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notifications_recipient", columnList = "recipient_id"))
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받는 회원(댓글 알림이면 상품 소유자). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Member recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /** 목록에 그대로 보여줄 렌더 스냅샷 문구(상품 삭제·개명에도 안정). */
    @Column(nullable = false, length = 500)
    private String message;

    /** 상품 식별자 스냅샷(연관 아님). 알림이 어떤 상품에서 비롯됐는지 + 코얼레싱 키. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private boolean isRead;

    /** 최근 알림 발생 시각. 목록 정렬 기준이자 코얼레싱 시 갱신 대상(createdAt과 분리). */
    @Column(nullable = false)
    private LocalDateTime lastNotifiedAt;

    protected Notification() {}

    public static Notification of(Member recipient, NotificationType type, String message, Long productId) {
        Notification notification = new Notification();
        notification.recipient = recipient;
        notification.type = type;
        notification.message = message;
        notification.productId = productId;
        notification.isRead = false;
        notification.lastNotifiedAt = LocalDateTime.now();
        return notification;
    }

    /**
     * 같은 상품에 새 이벤트가 다시 온 경우의 코얼레싱: 새 row 대신 발생 시각만 현재로 끌어올린다.
     * 안읽은 알림에만 적용된다(읽은 알림은 조회 대상에서 제외되어 새 알림이 생성됨).
     */
    public void renotify() {
        this.lastNotifiedAt = LocalDateTime.now();
    }

    public void markRead() {
        this.isRead = true;
    }

    public Long getId() { return id; }
    public Member getRecipient() { return recipient; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public Long getProductId() { return productId; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getLastNotifiedAt() { return lastNotifiedAt; }

    /** 연관 프록시의 식별자만 반환한다(식별자 접근은 프록시 초기화를 유발하지 않음). */
    public Long getRecipientId() { return recipient.getId(); }
}
