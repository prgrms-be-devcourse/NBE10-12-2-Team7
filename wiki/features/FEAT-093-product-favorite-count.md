---
id: FEAT-093
type: feature
status: done
author: han95white
date: 2026-07-02
related: []
tags: [product, backend]
pr: 93
---

## 무엇을 / 왜

 [Product] 관심 수 favoriteCount 이벤트 리스너 및 원자 업데이트 추가

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `product` 도메인 — ProductResponse(DTO, 수정), ProductSummaryResponse(DTO, 수정), Product(엔티티, 신규), ProductFavoriteCountHandler(기타, 신규), ProductRepository(리포지토리, 신규)
- 테스트 — 2개 파일 (+174줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/dto/ProductResponse.java` (+9/-1)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSummaryResponse.java` (+8/-1)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+8/-0)
- `backend/src/main/java/com/dongnemarket/product/event/ProductFavoriteCountHandler.java` (+28/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+11/-0)
- `backend/src/test/java/com/dongnemarket/product/integration/ProductFavoriteCountHandlerIntegrationTest.java` (+83/-0)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+91/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/93
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/91
