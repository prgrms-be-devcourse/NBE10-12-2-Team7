---
id: FEAT-074
type: feature
status: done
author: han95white
date: 2026-06-29
related: []
tags: [product, backend, docs]
pr: 74
---

## 무엇을 / 왜

내 상품 조회 API 주소 이관

## 어떻게 (구현 요약)

- 내 상품 조회 API 주소를 GET /api/members/me/products에서 GET /api/products/me로 이관
- 기존 주소 호환 매핑 제거
- /api/products/me가 공개 상품 상세 보안 패턴과 겹치는 경우를 대비해 인증 사용자 ID 누락 시 UNAUTHORIZED 처리
- Product/Postman/AI 문서의 담당 API 주소 갱신

**검증**

- sh ./gradlew clean test

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/MyProductController.java` (+1/-1)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+7/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+3/-3)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+10/-0)
- `docs/ai/00-ai-common-rules.md` (+31/-9)
- `docs/ai/02-product-category-agent.md` (+1/-1)
- `docs/postman/product-my-list.md` (+4/-4)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/74
- 이슈: 없음
