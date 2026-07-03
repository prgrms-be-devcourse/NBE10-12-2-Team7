package com.dongnemarket.chat.dto;

import com.dongnemarket.chat.entity.ChatRoom;

/** 채팅방 입장 응답. 상품 상세 + 판매자 정보. 방 생성(get-or-create) 및 입장 시 반환한다. */
public class ChatRoomDetailResponse {

    private final Long roomId;
    private final ChatProductDetail product;
    private final ChatMemberSummary seller;

    private ChatRoomDetailResponse(Long roomId, ChatProductDetail product, ChatMemberSummary seller) {
        this.roomId = roomId;
        this.product = product;
        this.seller = seller;
    }

    public static ChatRoomDetailResponse of(ChatRoom room) {
        return new ChatRoomDetailResponse(
                room.getId(),
                ChatProductDetail.from(room.getProduct()),
                ChatMemberSummary.of(room.getSeller())
        );
    }

    public Long getRoomId() { return roomId; }
    public ChatProductDetail getProduct() { return product; }
    public ChatMemberSummary getSeller() { return seller; }
}
