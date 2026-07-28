---
id: FEAT-103
type: feature
status: done
author: Crispy-down
date: 2026-07-03
related: []
tags: [favorite, backend, refactor]
pr: 103
---

## 무엇을 / 왜

관심목록 조회 N+1 제거 — fetch join + 상한 200

## 어떻게 (구현 요약)

- `GET /api/members/me/favorites`(내 관심목록 조회)의 **N+1 쿼리 제거** 리팩터 (기능·응답 계약 변경 없음)
- 과도하게 길던 파생 쿼리(`findAllByMember_Id...OrderByCreatedAtDescIdDesc`)를 `@Query` **fetch join**으로 전환 → 상품 요약을 **단일 쿼리**로 로딩
- `Limit(200)` 파라미터로 **서버 상한** 추가 — 개인 목록은 자연히 바운드되지만 비정상 폭주 시 payload·메모리를 최근순 200건으로 캡
- 정렬(`created_at DESC, id DESC`)·정책 A(삭제·숨김 제외) 필터는 그대로 유지
- 페이지네이션은 **클라이언트 사이드**에서 처리(개인 목록이라 서버 페이징 불필요)

**검증**

- `FavoriteControllerTest`(DB 통합) 전체 그린 — 정렬·정책 A 필터·응답 shape 회귀 없음 확인
- 생성 SQL 검증(`format_sql`):
  - `from favorites join products on ... where member_id=? and deleted_at is null and hidden=false` → **단일 쿼리로 상품 함께 로딩(N+1 소멸)**
  - `fetch first ? rows only` → LIMIT이 **SQL 레벨**에서 적용(`@ManyToOne` 단건 연관이라 메모리 페이징 아님)
- `./gradlew test --tests "com.dongnemarket.favorite.*"` BUILD SUCCESSFUL

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/repository/FavoriteRepository.java` (+13/-4)
- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+7/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **대안 비교 근거**: QueryDSL projection(프로젝트 미도입 → 의존성 비용 과함)·`default_batch_fetch_size`(전역 yml·인프라 소관·완화일 뿐 제거 아님)보다, 이 케이스(페이지네이션 없는 `@ManyToOne` 단건 연관 + 필터가 자식 상품에 걸림)에선 fetch join이 최소 변경으로 N+1을 완전히 제거하는 targeted fix라 선택
- **상한 200**은 클라이언트 페이징 전제하의 폭주 방어값 — 조정 의견 환영
- 응답 shape 불변이라 프론트(#99) 영향 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/103
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/102
