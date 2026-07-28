---
id: FEAT-133
type: feature
status: done
author: Crispy-down
date: 2026-07-06
related: []
tags: [favorite, backend]
pr: 133
---

## 무엇을 / 왜

관심 목록 상품 요약에 썸네일 URL 추가

## 어떻게 (구현 요약)

- 관심 목록(`GET /api/members/me/favorites`) 응답의 상품 요약 `FavoriteProductSummary`에 **대표 이미지(`thumbnailUrl`)** 필드 추가.
- `FavoriteProductSummary.from(Product)`에서 `product.getThumbnailUrl()` 매핑. 채팅 `ChatProductSummary` 패턴 재사용.
- 조회 쿼리(`findMyFavoritesWithProduct`)가 이미 `JOIN FETCH f.product`로 상품을 로딩 → **추가 쿼리·N+1 없음**. ErrorCode 신규 없음.

**검증**

- `FavoriteControllerTest` 관심 목록 조회 성공 케이스에 `product.thumbnailUrl` 단언 추가(setup에서 썸네일 세팅).
- `FavoriteControllerTest` 전체 그린(BUILD SUCCESSFUL).
- Postman 수동 테스트 예정 — 사용자 첨부

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/dto/FavoriteProductSummary.java` (+7/-3)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+5/-3)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 응답 shape에 `product.thumbnailUrl` 필드가 추가되는 **하위호환 확장**(기존 필드 불변).
- 프론트 관심 페이지 썸네일 렌더(`frontend/src/app/favorites/page.tsx`)는 **서유진 담당(FE)** — 본 PR은 BE DTO만. FE 미연동 시 화면엔 아직 안 뜸.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/133
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/132
