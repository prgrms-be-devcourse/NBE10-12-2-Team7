package com.dongnemarket.chat.repository;

import com.dongnemarket.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * (상품, 구매자) 쌍으로 기존 방을 찾는다. get-or-create의 "get" 단계.
     * {@code UNIQUE(product_id, buyer_id)} 제약과 1:1로 대응하므로 결과는 최대 한 건이다.
     */
    Optional<ChatRoom> findByProduct_IdAndBuyer_Id(Long productId, Long buyerId);

    /**
     * 내가 참여한(구매자 또는 판매자) 방 목록을 최근 생성순으로 조회한다.
     * <p>참여자 필터({@code buyer.id}/{@code seller.id})는 chat_rooms 컬럼이라 조인 없이 처리된다
     * (seller를 방에 저장한 덕분 — 인가/필터가 순수 row 비교).
     * 목록 렌더링에 필요한 상품·상대방을 {@code JOIN FETCH}로 함께 로딩해 N+1을 방지한다.
     * (정렬 기준을 마지막 메시지 시각으로 바꾸는 건 last-message 비정규화가 필요한 PR3 범위.)
     */
    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.product " +
            "JOIN FETCH r.buyer " +
            "JOIN FETCH r.seller " +
            "WHERE r.buyer.id = :memberId OR r.seller.id = :memberId " +
            "ORDER BY r.id DESC")
    List<ChatRoom> findMyChatRooms(@Param("memberId") Long memberId);

    /**
     * (상품, 구매자)로 방을 입장 상세와 함께 조회한다. 입장 응답(상품 상세·판매자)에 필요한 product·seller를 fetch join.
     * get-or-create 직후 반환용.
     */
    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.product " +
            "JOIN FETCH r.seller " +
            "WHERE r.product.id = :productId AND r.buyer.id = :buyerId")
    Optional<ChatRoom> findDetailByProductAndBuyer(@Param("productId") Long productId, @Param("buyerId") Long buyerId);
}
