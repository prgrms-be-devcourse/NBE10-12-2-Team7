---
id: FEAT-042
type: feature
status: done
author: han95white
date: 2026-06-26
related: []
tags: [product, backend]
pr: 42
---

## 무엇을 / 왜

접근 가능한 상품 검증 추가

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `product` 도메인 — ProductRepository(리포지토리, 신규), ProductService(서비스, 신규)
- 테스트 — 2개 파일 (+101줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+3/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+6/-0)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+61/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+40/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/42
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/34
