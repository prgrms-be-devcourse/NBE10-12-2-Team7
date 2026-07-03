package com.dongnemarket.chat.dto;

import com.dongnemarket.chat.entity.ChatMessage;

import java.time.LocalDateTime;

/** 메시지 한 건 응답. senderId로 내 메시지/상대 메시지(좌우 말풍선)를 구분한다. */
public class ChatMessageResponse {

    private final Long messageId;
    private final Long senderId;
    private final String content;
    private final LocalDateTime createdAt;

    private ChatMessageResponse(Long messageId, Long senderId, String content, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderId(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    public Long getMessageId() { return messageId; }
    public Long getSenderId() { return senderId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
