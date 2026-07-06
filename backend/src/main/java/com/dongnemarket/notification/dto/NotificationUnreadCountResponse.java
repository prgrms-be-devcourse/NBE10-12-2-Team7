package com.dongnemarket.notification.dto;

/** 안읽은 알림 총 개수(댓글 안읽음 + 안읽은 채팅방 수). 헤더 배지용. */
public class NotificationUnreadCountResponse {

    private final long unreadCount;

    private NotificationUnreadCountResponse(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public static NotificationUnreadCountResponse of(long unreadCount) {
        return new NotificationUnreadCountResponse(unreadCount);
    }

    public long getUnreadCount() { return unreadCount; }
}
