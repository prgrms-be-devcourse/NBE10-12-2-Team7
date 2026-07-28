---
id: FEAT-062
type: feature
status: done
author: Crispy-down
date: 2026-06-29
related: []
tags: [favorite, backend, docs]
pr: 62
---

## 무엇을 / 왜

내 관심 상품 목록 조회 API 구현

## 어떻게 (구현 요약)

- 내 관심 상품 목록 조회 API (`GET /api/members/me/favorites`)
- 인증 필요 — `@AuthenticationPrincipal`로 본인 관심 목록만 조회
- 최근 등록순 정렬: `created_at DESC, id DESC` (보조키로 결정적 순서 보장)
- 관심 상품이 없으면 빈 배열 `[]` 반환
- `ApiResponse<List<FavoriteResponse>>`(200) 반환, Swagger 문서화

**검증**

- `FavoriteServiceTest` 목록 2건 추가 (성공·최근등록순 / 빈 목록)
- `FavoriteControllerTest` 목록 3건 추가 (성공·최근등록순 / 빈 배열 / 401)
- favorite 도메인 테스트 전체 그린 (Service 8 · Controller 10 · Repository 3)
- Postman 수동 테스트 완료 (관심 2개 등록 → 최신순 조회 / 빈 목록 / 토큰 없음 401)
<img width="497" height="814" alt="image" src="https://github.com/user-attachments/assets/7e7af4e3-3e27-4ffc-954e-970563e0847a" />
<img width="608" height="757" alt="image" src="https://github.com/user-attachments/assets/6db798a4-c732-4cda-bf67-85fffaeaf0ad" />
<img width="749" height="750" alt="image" src="https://github.com/user-attachments/assets/7582b0c2-3b61-4237-aeb7-f6a7d0156dd1" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/controller/FavoriteController.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/favorite/repository/FavoriteRepository.java` (+1/-1)
- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+9/-0)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+40/-0)
- `backend/src/test/java/com/dongnemarket/favorite/service/FavoriteServiceTest.java` (+27/-0)
- `docs/postman/favorite-list.md` (+83/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- MVP 범위로 기존 `FavoriteResponse` DTO 그대로 사용 (상품 제목/가격/거래상태 요약, 삭제·숨김 상품 필터 정책은 팀 회의 후 별도 PR).
- 정렬 보조키(`id DESC`)에 대한 성능 검토: `created_at`만으로는 초 단위 동률 시 순서가 비결정적이라 `id`를 보조키로 추가함. 현재 인덱스(PK `id`, UNIQUE `(member_id, product_id)`)에서 이 쿼리는 정렬키 1개든 2개든 어차피 filesort이고, 한 회원의 관심 행 수가 작아 비용은 무시 수준. 보조키 비교는 동률에서만 발생해 추가 비용 사실상 없음. 향후 트래픽 증가 시 `(member_id, created_at)` 인덱스를 추가하면 InnoDB가 PK를 trailing으로 포함해 `created_at DESC, id DESC`를 filesort 없이 역방향 스캔으로 처리 가능 → 보조키가 오히려 유리. 인덱스는 favorites 테이블 DDL 변경이라 선제 추가하지 않고 필요 시 별도 협의.
- 조회 API라 products/members 테이블은 변경하지 않음 (담당 도메인 외 변경 없음).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/62
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/61
