package com.dongnemarket.chat.dto;

import com.dongnemarket.chat.entity.ChatMessage;
import com.dongnemarket.chat.entity.ChatRoom;
import com.dongnemarket.member.entity.Member;

import java.time.LocalDateTime;

/** 내 채팅방 목록의 한 행. 상품 요약 + 상대방 + 방 생성일 + 마지막 메시지(없으면 null) + 안읽음 수. */
public class ChatRoomListResponse {

    private final Long roomId;
    private final ChatProductSummary product;
    private final ChatMemberSummary opponent;
    /** 요청자(나) 기준 역할. "BUYER" 또는 "SELLER" — 매너온도 후기 등록 등 구매자 전용 UI 노출 여부 판단용. */
    private final String viewerRole;
    private final LocalDateTime createdAt;
    private final ChatMessageResponse lastMessage;
    private final long unreadCount;

    private ChatRoomListResponse(Long roomId, ChatProductSummary product, ChatMemberSummary opponent, String viewerRole,
                                LocalDateTime createdAt, ChatMessageResponse lastMessage, long unreadCount) {
        this.roomId = roomId;
        this.product = product;
        this.opponent = opponent;
        this.viewerRole = viewerRole;
        this.createdAt = createdAt;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
    }

    /**
     * opponent는 요청자 기준 상대방(구매자면 판매자, 판매자면 구매자), lastMessage는 없으면 null.
     * viewerRole은 요청자 본인의 역할("BUYER"/"SELLER"). unreadCount는 요청자가 아직 읽지 않은 상대 메시지 수(목록 배지용).
     */
    public static ChatRoomListResponse of(ChatRoom room, Member opponent, String viewerRole,
                                          ChatMessage lastMessage, long unreadCount) {
        return new ChatRoomListResponse(
                room.getId(),
                ChatProductSummary.from(room.getProduct()),
                ChatMemberSummary.of(opponent),
                viewerRole,
                room.getCreatedAt(),
                lastMessage == null ? null : ChatMessageResponse.from(lastMessage),
                unreadCount
        );
    }

    public Long getRoomId() { return roomId; }
    public ChatProductSummary getProduct() { return product; }
    public ChatMemberSummary getOpponent() { return opponent; }
    public String getViewerRole() { return viewerRole; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public ChatMessageResponse getLastMessage() { return lastMessage; }
    public long getUnreadCount() { return unreadCount; }
}
