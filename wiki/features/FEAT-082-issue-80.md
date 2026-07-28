---
id: FEAT-082
type: feature
status: done
author: Crispy-down
date: 2026-07-01
related: []
tags: [comment, favorite, backend, docs]
pr: 82
---

## 무엇을 / 왜

[Refactor] Favorite·Comment 연관관계 도입 + 검증 중앙화 + 관심목록 상품요약 (#80)

## 어떻게 (구현 요약)

- Favorite·Comment 엔티티 `@ManyToOne(LAZY)` member·product 도입 (컬럼명 유지, DDL 없음)
  - FK 참조는 findById 시맨틱(`EntityManager.find`) — 타 도메인 Repository 주입을 피하려 Repo 대신 EntityManager 사용
- 상품 검증 중앙화: `existsById` → `productService.validateAccessibleProduct`
  - 관심 등록 / 댓글 작성 / 댓글 목록 조회 → 삭제·숨김 상품 404 (결정 D1)
- 관심목록 상품요약 확장: `MyFavoriteResponse` / `FavoriteProductSummary`
  - 삭제·숨김 제외(정책 A)는 **파생 쿼리**로 DB에서 필터, 상품 연관은 LAZY
  - 관심 등록(POST) 응답(`FavoriteResponse`)은 기존 계약 유지

**검증**

- Favorite·Comment 단위/통합 테스트 전부 통과 (숨김·삭제 404, 목록 제외 등 엣지 포함)
- 전체 스위트 그린 (단, 아래 admin 2개 파일을 제외하고 실행 — 확인필요 참고)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/entity/Comment.java` (+17/-9)
- `backend/src/main/java/com/dongnemarket/comment/repository/CommentRepository.java` (+1/-1)
- `backend/src/main/java/com/dongnemarket/comment/service/CommentService.java` (+19/-14)
- `backend/src/main/java/com/dongnemarket/favorite/controller/FavoriteController.java` (+4/-3)
- `backend/src/main/java/com/dongnemarket/favorite/dto/FavoriteProductSummary.java` (+41/-0)
- `backend/src/main/java/com/dongnemarket/favorite/dto/MyFavoriteResponse.java` (+31/-0)
- `backend/src/main/java/com/dongnemarket/favorite/entity/Favorite.java` (+17/-9)
- `backend/src/main/java/com/dongnemarket/favorite/repository/FavoriteRepository.java` (+8/-3)
- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+30/-15)
- `backend/src/test/java/com/dongnemarket/comment/CommentRepositoryTest.java` (+0/-63)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+49/-10)
- `backend/src/test/java/com/dongnemarket/comment/service/CommentServiceTest.java` (+45/-23)
- `backend/src/test/java/com/dongnemarket/favorite/FavoriteRepositoryTest.java` (+0/-52)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+73/-3)
- `backend/src/test/java/com/dongnemarket/favorite/service/FavoriteServiceTest.java` (+56/-24)
- … 외 4개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- ⚠️ **머지 순서 의존**: `Comment.of` 시그니처 변경으로 admin 댓글 관리 테스트
  (`AdminCommentControllerTest` / `AdminCommentServiceTest`)가 **컴파일 실패**한다.
  팀장과 협의 완료, admin 테스트 리팩터는 async 진행 중.
  → 이 브랜치가 admin 수정보다 **먼저 머지되면 develop 빌드가 깨진다.** 머지 순서 조율 필요.
- ⚠️ **API 파괴 변경**: `GET /api/members/me/favorites` 응답 shape 변경
  (`id`→`favoriteId`, `productId`→`product.productId`, 상품요약 중첩) → 프론트(#71) 대응 필요.
- 🔧 **N+1 완화 — 민석님 진행 필요**: 관심목록이 상품을 LAZY 로딩하여 N+1이 발생한다
  (개인 관심목록이라 N이 작아 기능상 허용). `hibernate.default_batch_fetch_size`(전역 `application.yml`)를
  설정하면 `IN` 절 배치로 완화되나, **전역 설정이라 본 PR에 미포함** — **민석님이 global 설정에 추가 필요.**
- 댓글 목록: 숨김·삭제 상품 조회가 기존 200 → 404 (결정 D1).
- 카운트(관심수·댓글수)는 본 PR 범위 밖 (한상민 협의 후 별도 이슈).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/82
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/80
