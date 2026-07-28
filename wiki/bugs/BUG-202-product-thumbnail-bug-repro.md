---
id: BUG-202
type: bug
status: done
author: han95white
date: 2026-07-08
related: []
tags: [product, backend]
pr: 202
---

## 증상

상품 수정 시 대표이미지 변경 미반영 수정 — 상세 증상 미기록.

## 원인

- PR 본문에 원인 기록 없음.

## 해결 방법

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `product` 도메인 — ProductService(서비스, 수정)
- 테스트 — 1개 파일 (+49줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+5/-3)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+49/-0)

## 재발 방지

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/202
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/201
