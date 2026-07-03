package com.dongnemarket.chat.dto;

import jakarta.validation.constraints.NotNull;

/** 채팅방 생성(get-or-create) 요청. Jackson 역직렬화를 위해 public 생성자를 둔다. */
public class ChatRoomCreateRequest {

    @NotNull
    private Long productId;

    public ChatRoomCreateRequest() {}

    public ChatRoomCreateRequest(Long productId) {
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
