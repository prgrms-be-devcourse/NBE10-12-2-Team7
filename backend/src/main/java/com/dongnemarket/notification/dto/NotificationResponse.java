package com.dongnemarket.notification.dto;

import com.dongnemarket.notification.entity.Notification;
import com.dongnemarket.notification.entity.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** 알림 목록 아이템 응답. 저장된 알림을 화면에 보여줄 최소 필드로 매핑한다(엔티티 직접 노출 금지). */
public class NotificationResponse {

    private final NotificationType type;
    private final String message;
    private final Long productId;
    private final boolean isRead;
    /** 최근 알림 발생 시각(정렬 기준). 코얼레싱된 알림은 마지막 발생 시각이다. */
    private final LocalDateTime occurredAt;

    private NotificationResponse(NotificationType type, String message, Long productId,
                                 boolean isRead, LocalDateTime occurredAt) {
        this.type = type;
        this.message = message;
        this.productId = productId;
        this.isRead = isRead;
        this.occurredAt = occurredAt;
    }

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getType(),
                notification.getMessage(),
                notification.getProductId(),
                notification.isRead(),
                notification.getLastNotifiedAt()
        );
    }

    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public Long getProductId() { return productId; }
    /** boolean getter의 기본 직렬화 키("read") 대신 설계상 명시 키 "isRead"로 노출한다. */
    @JsonProperty("isRead")
    public boolean isRead() { return isRead; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
