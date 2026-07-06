package com.dongnemarket.global.common.event;

import java.math.BigDecimal;

/**
 * 상품 가격이 변경될 때 발행되는 도메인 이벤트.
 * <p>notification 도메인이 수신하여 해당 상품에 <b>채팅방을 연 구매자들</b>에게 가격 변경 알림을 저장한다(상품별 코얼레싱).
 * 발행은 상품 수정 커밋 이후({@code AFTER_COMMIT}) 별도 트랜잭션에서 처리되므로,
 * 알림 저장 실패가 상품 수정을 롤백하지 않는다(best-effort).
 *
 * @param productId     가격이 변경된 상품 id
 * @param productTitle  알림 문구 렌더용 상품 제목 스냅샷(소비 측이 Product를 재조회하지 않도록 전달)
 * @param oldPrice      변경 전 가격
 * @param newPrice      변경 후 가격
 */
public record ProductPriceChangedEvent(Long productId, String productTitle,
                                       BigDecimal oldPrice, BigDecimal newPrice) {
}
