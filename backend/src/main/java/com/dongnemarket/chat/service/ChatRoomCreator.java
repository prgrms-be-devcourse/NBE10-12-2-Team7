package com.dongnemarket.chat.service;

import com.dongnemarket.chat.entity.ChatRoom;
import com.dongnemarket.chat.repository.ChatRoomRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅방 get-or-create의 <b>쓰기 트랜잭션</b>만 담당한다(ChatService에서 분리한 별도 빈).
 * <p><b>왜 분리했나</b>: 방을 저장하다 UNIQUE(product_id, buyer_id) 경쟁에 지면 INSERT가 실패하고
 * 이 트랜잭션은 rollback-only가 된다. 같은 트랜잭션 안에서 "이긴 방"을 재조회해 반환하면 커밋 시점에
 * {@code UnexpectedRollbackException}(500)이 난다. 따라서 쓰기를 독립 트랜잭션 경계로 두고,
 * 경쟁 복구(재조회)는 이 경계 <b>바깥</b>(ChatService)에서 새 트랜잭션으로 수행한다.
 */
@Component
public class ChatRoomCreator {

    private final ChatRoomRepository chatRoomRepository;
    private final EntityManager entityManager;

    public ChatRoomCreator(ChatRoomRepository chatRoomRepository, EntityManager entityManager) {
        this.chatRoomRepository = chatRoomRepository;
        this.entityManager = entityManager;
    }

    /**
     * (상품, 구매자) 방이 없으면 생성한다. 이미 있으면 아무 것도 하지 않는다.
     * 판매자는 상품 소유자에서 파생해 스냅샷 저장하고, 자기 상품이면 차단한다.
     * 동시 최초 생성 경쟁 시 진 쪽은 UNIQUE 위반으로 {@code DataIntegrityViolationException}을 던진다(호출자가 복구).
     */
    @Transactional
    public void createIfAbsent(Long memberId, Long productId) {
        if (chatRoomRepository.findByProduct_IdAndBuyer_Id(productId, memberId).isPresent()) {
            return;
        }
        Product product = entityManager.find(Product.class, productId);
        Member seller = product.getMember();
        if (seller.getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }
        Member buyer = entityManager.getReference(Member.class, memberId);
        chatRoomRepository.save(ChatRoom.of(product, buyer, seller));
    }
}
