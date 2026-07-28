---
id: FEAT-165
type: feature
status: done
author: Crispy-down
date: 2026-07-06
related: []
tags: [chat, global, notification, product, backend]
pr: 165
---

## 무엇을 / 왜

구매 상품 가격 수정 알림 (PR-N3)

## 어떻게 (구현 요약)

**채팅방을 연 상품의 가격이 변경되면 그 구매자들에게 알림**을 저장한다(저장형·상품별 코얼레싱).
크로스도메인 협의하에 **발행+소비를 한 PR로** 구현(분할 시 생기는 이벤트파일 중복충돌·머지순서 빌드깨짐·필드계약 어긋남을 원천 제거).
- **발행(Product)**: `ProductService.updateProduct`에서 `update()` 전 old price 캡처 → `compareTo(...) != 0`일 때만 `ProductPriceChangedEvent` 발행. (BigDecimal은 scale 민감이라 `equals` 아닌 `compareTo`.)
- **이벤트**: `ProductPriceChangedEvent(productId, productTitle, oldPrice, newPrice)` @ `global.common.event`.
- **소비(Notification)**: `NotificationEventHandler.handlePriceChanged` = `AFTER_COMMIT` + `REQUIRES_NEW`(best-effort). `NotificationService.notifyPriceChange`가 채팅방 buyer들에게 상품별 코얼레싱 저장(`notifyComment`와 코얼레싱 로직 공유).
- 수신자: `ChatService.findBuyerIdsForProduct` → `ChatRoomRepository.findBuyerIdsByProduct_Id`. 판매자는 자기 상품 구매 불가라 제외.
- `NotificationType`/`NotificationFeedType`에 `PRICE_CHANGE` 추가. 저장형이라 피드/`unread-count`에 자동 반영.
- 문구: `"{상품명}"의 가격이 변경되었습니다.` (가격 수치 미포함 — 코얼레싱 시 `renotify()`가 message를 갱신하지 않아 옛 가격이 남는 것을 방지.)

**검증**

- **`./gradlew test` 전체 그린**(BUILD SUCCESSFUL).
- `NotificationPriceChangeTest`(통합, `updateProduct` 실제 호출 → AFTER_COMMIT E2E): 채팅 buyer에게 알림 / 가격 그대로면 미발생 / **구매자 여럿→각각** / 채팅방 없는 구매자 제외 / **재변경 코얼레싱 1행** / 피드에 PRICE_CHANGE 노출.
- `ProductServiceTest`(단위): 가격 변경 시 발행 / 가격 동일(800000 vs 800000.00)이면 미발행(compareTo 검증).
- Postman 수동 테스트 예정 — 사용자 첨부
<img width="688" height="727" alt="image" src="https://github.com/user-attachments/assets/04f0b158-c20a-4311-9228-24c12c1385ba" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/repository/ChatRoomRepository.java` (+7/-0)
- `backend/src/main/java/com/dongnemarket/chat/service/ChatService.java` (+6/-0)
- `backend/src/main/java/com/dongnemarket/global/common/event/ProductPriceChangedEvent.java` (+18/-0)
- `backend/src/main/java/com/dongnemarket/notification/dto/NotificationFeedType.java` (+2/-1)
- `backend/src/main/java/com/dongnemarket/notification/dto/NotificationResponse.java` (+2/-2)
- `backend/src/main/java/com/dongnemarket/notification/entity/NotificationType.java` (+5/-5)
- `backend/src/main/java/com/dongnemarket/notification/event/NotificationEventHandler.java` (+7/-0)
- `backend/src/main/java/com/dongnemarket/notification/service/NotificationService.java` (+28/-3)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+14/-1)
- `backend/src/test/java/com/dongnemarket/notification/service/NotificationPriceChangeTest.java` (+180/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+36/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **⚠️ 크로스도메인 수정: `ProductService.updateProduct`** — 절대규칙 1번 예외로, **한상민님과 협의하에** 권건우가 가격 변경 감지·발행을 추가했습니다. 해당 diff 리뷰 부탁드립니다.
- **팀장님 승인 요청**: `global/common/event/ProductPriceChangedEvent.java` 추가(팀장 영역). favoriteCount·CommentCreatedEvent 선례와 동일 위치입니다 — 위치 승인 부탁드립니다.
- `ProductServiceTest`에 `@Mock ApplicationEventPublisher` 1줄 추가(생성자 파라미터 증가로 기존 테스트 NPE 방지).
- 설계 상세: `docs/design/notification-price-change-collaboration.md`.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/165
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/140
