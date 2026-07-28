---
id: FEAT-233
type: feature
status: done
author: Crispy-down
date: 2026-07-27
related: [FEAT-242]
tags: [favorite, notification, backend]
pr: 233
---

## 무엇을 / 왜

찜한 사용자에게 상품 가격 변경 알림 발송

## 어떻게 (구현 요약)

- 상품 가격 변경 알림 수신자를 **채팅방을 연 구매자 ∪ 관심(찜) 등록 사용자** 합집합으로 확대
- `FavoriteRepository.findFavoriteMemberIdsForProduct`: 상품을 찜한 회원 id 조회 (판매자 본인 제외 — 자기 상품 찜이 가능하므로 쿼리 레벨 제외가 필수)
- `FavoriteService`에 조회 메서드 노출 (`ChatService.findBuyerIdsForProduct` 패턴 대칭)
- `NotificationService.notifyPriceChange`: `LinkedHashSet`으로 두 집합 합집합에 발송, 중복 사용자는 코얼레싱으로 1행 수렴

**검증**

- `NotificationPriceChangeTest`에 통합 테스트 3개 추가:
  - 채팅방 없이 찜만 한 사용자도 가격 변경 시 알림 수신
  - 판매자가 자기 상품을 찜해도 자기 가격 변경 알림 미수신 (판매자 제외 검증)
  - 채팅·찜 둘 다인 사용자는 알림 정확히 1건 (합집합·코얼레싱)
- `./gradlew test` (notification·favorite·chat) 전체 그린. @SpringBootTest 부팅 성공으로 순환참조 없음 확인.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/repository/FavoriteRepository.java` (+9/-0)
- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+5/-0)
- `backend/src/main/java/com/dongnemarket/notification/service/NotificationService.java` (+15/-4)
- `backend/src/test/java/com/dongnemarket/notification/service/NotificationPriceChangeTest.java` (+44/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- BE only, FE 무관: 기존 `PRICE_CHANGE` 타입·`NotificationResponse` 형태 그대로이고 **수신자 집합만 확대**됨
- ErrorCode 신규 없음
- 의존 방향: `NotificationService → FavoriteService → ProductService` (역방향 없음, 순환 아님)
- 판매자 제외 쿼리 조건(`<> f.product.member.id`)은 load-bearing — Javadoc에 제거 금지 명시

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/233
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/232
