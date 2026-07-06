package com.dongnemarket.notification.dto;

import com.dongnemarket.notification.entity.Notification;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 알림 피드 아이템 응답. 저장형(댓글)과 파생형(채팅)을 하나의 shape으로 노출한다(엔티티 직접 노출 금지).
 * <p>{@code roomId}는 채팅 알림에서만 채워지며(방 이동용), 댓글 알림에선 null이라 직렬화에서 생략된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private final NotificationFeedType type;
    private final String message;
    private final Long productId;
    /** 채팅 알림에서만 세팅(해당 방으로 이동). 댓글 알림은 null. */
    private final Long roomId;
    private final boolean isRead;
    /** 최근 발생 시각(정렬 기준). 댓글은 코얼레싱된 마지막 발생 시각, 채팅은 마지막 메시지 시각. */
    private final LocalDateTime occurredAt;

    private NotificationResponse(NotificationFeedType type, String message, Long productId,
                                 Long roomId, boolean isRead, LocalDateTime occurredAt) {
        this.type = type;
        this.message = message;
        this.productId = productId;
        this.roomId = roomId;
        this.isRead = isRead;
        this.occurredAt = occurredAt;
    }

    /** 저장된 알림(댓글·가격변경) → 피드 아이템. 저장 타입을 동일 이름의 피드 타입으로 매핑한다. */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                NotificationFeedType.valueOf(notification.getType().name()),
                notification.getMessage(),
                notification.getProductId(),
                null,
                notification.isRead(),
                notification.getLastNotifiedAt()
        );
    }

    /** 안읽은 채팅방에서 파생한 채팅 알림 → 피드 아이템(저장 안 함, 항상 안읽음). */
    public static NotificationResponse chat(String message, Long productId, Long roomId, LocalDateTime occurredAt) {
        return new NotificationResponse(NotificationFeedType.CHAT, message, productId, roomId, false, occurredAt);
    }

    public NotificationFeedType getType() { return type; }
    public String getMessage() { return message; }
    public Long getProductId() { return productId; }
    public Long getRoomId() { return roomId; }
    /** boolean getter의 기본 직렬화 키("read") 대신 설계상 명시 키 "isRead"로 노출한다. */
    @JsonProperty("isRead")
    public boolean isRead() { return isRead; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
