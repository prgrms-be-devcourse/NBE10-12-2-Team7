package com.dongnemarket.chat.repository;

/**
 * 방별 안읽음 메시지 수 프로젝션. {@link ChatMessageRepository#countUnreadPerRoom} 결과의 한 행이다.
 * 안읽음이 0인 방은 GROUP BY 결과에 나타나지 않으므로 서비스에서 기본값 0으로 채운다.
 */
public interface RoomUnreadCount {
    Long getRoomId();
    long getUnreadCount();
}
