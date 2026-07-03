package com.dongnemarket.chat.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 채팅 메시지 한 건. 방(ChatRoom)에 속하며 보낸 사람(sender)과 내용을 가진다.
 * <p>메시지는 방과 달리 계속 쌓이는(성장하는) 데이터라 별도 엔티티로 분리하고, 커서 페이지네이션으로 조회한다.
 * 상품이 삭제·숨김돼도 대화 기록은 보존해야 하므로 상품/방으로부터의 cascade 삭제는 두지 않는다.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Column(nullable = false, length = 1000)
    private String content;

    protected ChatMessage() {}

    public static ChatMessage of(ChatRoom chatRoom, Member sender, String content) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = sender;
        message.content = content;
        return message;
    }

    public Long getId() { return id; }
    public ChatRoom getChatRoom() { return chatRoom; }
    public Member getSender() { return sender; }
    public String getContent() { return content; }

    /** 연관 프록시의 식별자만 반환한다(식별자 접근은 프록시 초기화를 유발하지 않음). */
    public Long getChatRoomId() { return chatRoom.getId(); }
    public Long getSenderId() { return sender.getId(); }
}
