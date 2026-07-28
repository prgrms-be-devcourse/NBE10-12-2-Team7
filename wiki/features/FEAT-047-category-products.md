---
id: FEAT-047
type: feature
status: done
author: han95white
date: 2026-06-26
related: []
tags: [category, product, backend, docs]
pr: 47
---

## 무엇을 / 왜

카테고리별 상품 목록 조회 API 구현

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `category` 도메인 — CategoryController(컨트롤러, 수정)
- `product` 도메인 — ProductRepository(리포지토리, 신규), ProductService(서비스, 신규)
- 문서 — `docs/postman/` 1개
- 추가된 API 표면 — `@GetMapping("/{categoryId}/products")`
- 테스트 — 3개 파일 (+165줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/category/controller/CategoryController.java` (+12/-1)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+3/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+11/-0)
- `backend/src/test/java/com/dongnemarket/category/controller/CategoryControllerTest.java` (+91/-0)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+44/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+30/-0)
- `docs/postman/category-products.md` (+113/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/47
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/43
