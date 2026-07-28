---
id: FEAT-075
type: feature
status: done
author: jomin4
date: 2026-06-30
related: []
tags: [product, admin, support, backend]
pr: 75
---

## 무엇을 / 왜

테스트 서버 구축

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `product` 도메인 — Product(엔티티, 수정)
- 테스트 — 5개 파일 (+119줄)

## 건드린 파일

- `backend/build.gradle` (+19/-2)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+3/-2)
- `backend/src/test/java/com/dongnemarket/admin/integration/AdminMemberIntegrationTest.java` (+52/-0)
- `backend/src/test/java/com/dongnemarket/support/BaseIntegrationTest.java` (+34/-0)
- `backend/src/test/resources/application-integration.yml` (+16/-0)
- `backend/src/test/resources/sql/clean-scenario.sql` (+8/-0)
- `backend/src/test/resources/sql/products-scenario.sql` (+9/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/75
- 이슈: 없음
