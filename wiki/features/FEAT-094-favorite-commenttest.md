---
id: FEAT-094
type: feature
status: done
author: Crispy-down
date: 2026-07-02
related: []
tags: [comment, favorite, backend, test]
pr: 94
---

## 무엇을 / 왜

테스트 유스케이스 중심 재구성 + 계층 책임 정리

## 어떻게 (구현 요약)

Favorite·Comment 도메인 테스트를 **유스케이스 중심**으로 재구성했습니다. 기능 변경 없음(테스트 코드만).

**관통 원칙**
- **컨트롤러(통합) = 실패·엣지 포함 모든 사용자향 유스케이스(= 설명서).** 통신만 `MockMvc`로 대체하고 Controller·Service·Repository는 실제로 동작(H2). — 현행 구조 유지, 재설계 안 함.
- **서비스(단위) = end-to-end로 비싸거나 불가능한 비자명 분기만.** 단순 매핑/조회는 컨트롤러 통합에 위임.
- `@Nested`로 유스케이스별 그룹핑 → 테스트가 스펙/목차처럼 읽히게.

**변경 요약**
| 파일 | 변경 |
|---|---|
| FavoriteServiceTest | 12 → 6 (매핑/조회 삭제, 발행/미발행을 흐름 테스트에 병합, race→`FAVORITE_ALREADY_EXISTS` 변환 유지) |
| FavoriteControllerTest | 14 → 15 (`@Nested` 등록/목록/취소, 중복 첫 요청 201 단언 보강, 변조 토큰 엣지) |
| CommentServiceTest | 11 → 8 (매핑/조회 삭제, 접근 게이트·작성자 검증·소프트삭제 유지) |
| CommentControllerTest | 18 → 21 (`@Nested` 작성/목록/수정/삭제, 엣지: 500자 초과·수정 공백·변조 토큰) |

**검증**

- `./gradlew test` **BUILD SUCCESSFUL** (전체 그린, 회귀 없음).
- **경험적으로 확인한 엣지**(추정 아님, 실제 실행값으로 단언):
  - 변조 토큰 → **`INVALID_TOKEN`(401)**
  - 댓글 500자 초과 → **400 `INVALID_INPUT_VALUE`** / 수정 공백 → **400**
- 테스트 전용 변경이라 Postman 대상 없음(API 동작 불변).

## 건드린 파일

- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+256/-200)
- `backend/src/test/java/com/dongnemarket/comment/service/CommentServiceTest.java` (+105/-125)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+199/-170)
- `backend/src/test/java/com/dongnemarket/favorite/service/FavoriteServiceTest.java` (+96/-167)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **범위 밖(의도적 제외)**:
  - **삭제·숨김 에러코드 세분화**(`HIDDEN_PRODUCT` 403 / `DELETED_PRODUCT` 404): `validateAccessibleProduct`는 정책 A(404 통일, 존재 정보 유출 방지)를 유지 → 현행 계약 그대로 검증. 세분화는 API 계약 변경 + ProductService(한상민) 소관이라 별도 논의.
    - 참고로 발견한 비대칭: ProductService **직접 접근** 메서드(`getProduct`/`update`/`delete`)는 삭제를 `DELETED_PRODUCT`로 **구분**하나, 찜·댓글용 `validateAccessibleProduct`는 `PRODUCT_NOT_FOUND`로 **통일**합니다. 의도된 익명화인지 확인 지점.
  - 정렬 타이브레이크(동일 `created_at`): 강제 셋업이 brittle·저ROI → known gap.
  - 카운트(favoriteCount) 통합 테스트: #86 소관.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/94
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/92
