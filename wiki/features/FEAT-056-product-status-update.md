---
id: FEAT-056
type: feature
status: done
author: han95white
date: 2026-06-27
related: []
tags: [product, backend, docs]
pr: 56
---

## 무엇을 / 왜

상품 거래 상태 변경 API 구현

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `product` 도메인 — ProductController(컨트롤러, 신규), ProductStatusUpdateRequest(DTO, 신규), Product(엔티티, 신규), ProductService(서비스, 신규)
- 문서 — `docs/ai/` 1개, `docs/postman/` 1개
- 추가된 API 표면 — `@PatchMapping("/{productId}/status")`
- 테스트 — 2개 파일 (+409줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductStatusUpdateRequest.java` (+17/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+4/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+34/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+245/-2)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+164/-0)
- `docs/ai/02-product-category-agent.md` (+74/-0)
- `docs/postman/product-status-update.md` (+277/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/56
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/54
