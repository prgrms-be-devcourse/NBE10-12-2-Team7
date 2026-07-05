package com.dongnemarket.chat.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 1:1 채팅방. 특정 상품에 대해 (구매자 ↔ 판매자) 한 쌍의 대화를 나타낸다.
 * <p>한 상품에 대해 한 구매자는 방을 하나만 가진다 → {@code UNIQUE(product_id, buyer_id)}로 보장하고,
 * 방 생성은 "있으면 반환, 없으면 생성"(get-or-create)으로 멱등하게 처리한다.
 * <p>판매자는 {@code product.member}에서 파생 가능하지만, 참여자 인가가 메시지 전송·조회마다 실행되므로
 * 방에 스냅샷으로 저장(비정규화)해 인가를 순수 컬럼 비교로 유지한다(파생 시 매 인가마다 Product 로딩 → N+1 방지).
 * 상품 소유권은 이전 기능이 없어 불변이라 드리프트 위험이 없다.
 */
@Entity
@Table(name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_rooms_product_buyer",
                columnNames = {"product_id", "buyer_id"}
        ))
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 대화를 시작한 구매자. 판매자가 아닌 상대방이라 파생 불가 → 명시적으로 저장한다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Member buyer;

    /** 상품 판매자의 스냅샷. 인가를 row 비교로 유지하기 위해 비정규화 저장(위 클래스 주석 참고). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    /**
     * 참여자별 마지막 읽은 메시지 id(구매자/판매자 각각). 1:1이라 참여자가 2명 고정이므로
     * 별도 읽음 테이블 대신 방에 컬럼 2개로 둔다(안읽음 카운트 쿼리를 방 컬럼 비교로 단순화).
     * null이면 아직 한 번도 읽지 않음 = 상대가 보낸 메시지 전부가 안읽음.
     */
    @Column(name = "buyer_last_read_message_id")
    private Long buyerLastReadMessageId;

    @Column(name = "seller_last_read_message_id")
    private Long sellerLastReadMessageId;

    protected ChatRoom() {}

    public static ChatRoom of(Product product, Member buyer, Member seller) {
        ChatRoom room = new ChatRoom();
        room.product = product;
        room.buyer = buyer;
        room.seller = seller;
        return room;
    }

    /**
     * 주어진 회원이 이 방의 참여자(구매자 또는 판매자)인지 판단한다.
     * buyer/seller 프록시의 식별자만 읽으므로 추가 로딩을 유발하지 않는다.
     */
    public boolean isParticipant(Long memberId) {
        return buyer.getId().equals(memberId) || seller.getId().equals(memberId);
    }

    /** 주어진 참여자의 마지막 읽은 메시지 id(구매자/판매자 좌석 분기). 아직 안 읽었으면 null. */
    public Long lastReadMessageIdOf(Long memberId) {
        return buyer.getId().equals(memberId) ? buyerLastReadMessageId : sellerLastReadMessageId;
    }

    /**
     * 참여자의 읽음 지점을 주어진 메시지 id까지 전진시킨다(해당 좌석 컬럼만 갱신).
     * 읽음 지점은 단조 전진만 하므로 이미 더 뒤를 읽은 상태면 무시한다(순서 뒤바뀐/중복 요청에 안전).
     * 참여자 인가는 서비스에서 선행하므로 비참여자 호출은 도달하지 않는다.
     */
    public void markRead(Long memberId, Long messageId) {
        if (buyer.getId().equals(memberId)) {
            if (buyerLastReadMessageId == null || messageId > buyerLastReadMessageId) {
                buyerLastReadMessageId = messageId;
            }
        } else if (seller.getId().equals(memberId)) {
            if (sellerLastReadMessageId == null || messageId > sellerLastReadMessageId) {
                sellerLastReadMessageId = messageId;
            }
        }
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Member getBuyer() { return buyer; }
    public Member getSeller() { return seller; }

    /** 연관 프록시의 식별자만 반환한다(식별자 접근은 프록시 초기화를 유발하지 않음). */
    public Long getProductId() { return product.getId(); }
    public Long getBuyerId() { return buyer.getId(); }
    public Long getSellerId() { return seller.getId(); }
}
