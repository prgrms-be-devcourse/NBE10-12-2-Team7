---
id: FEAT-068
type: feature
status: done
author: han95white
date: 2026-06-29
related: []
tags: [product, backend]
pr: 68
---

## 무엇을 / 왜

상품 검색 API 구현

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `product` 도메인 — ProductController(컨트롤러, 신규), ProductSearchRequest(DTO, 신규), ProductRepository(리포지토리, 수정), ProductSpecification(리포지토리, 신규), ProductService(서비스, 신규)
- 추가된 API 표면 — `@GetMapping("/search")`
- 테스트 — 3개 파일 (+255줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+14/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSearchRequest.java` (+38/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+2/-1)
- `backend/src/main/java/com/dongnemarket/product/repository/spec/ProductSpecification.java` (+80/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+53/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+89/-0)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+78/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+88/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/68
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/65
