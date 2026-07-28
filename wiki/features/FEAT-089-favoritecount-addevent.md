---
id: FEAT-089
type: feature
status: done
author: Crispy-down
date: 2026-07-01
related: [FEAT-085, FEAT-141]
tags: [admin, global, backend]
pr: 89
---

## 무엇을 / 왜

[Feature] 찜 카운트용 도메인 이벤트 2종 추가 + Report 리팩터 빌드 복구 (#86)

## 어떻게 (구현 요약)

- **찜 카운트 이벤트 계약**: `global/common/event`에 `FavoriteAddedEvent(Long productId)`, `FavoriteRemovedEvent(Long productId)` (record) 추가.
  - Product가 수신해 `favoriteCount`를 증감시키기 위한 계약. 발행(FavoriteService)·리스너(Product)는 후속 PR (이슈 #86).
- **(빌드 복구) Report @ManyToOne 리팩터(#85) 파급 수정** — 팀장 요청/협의:
  - `AdminReportResponse`: 사라진 Long 게터 참조 → 연관 경로(`getReporter().getId()` 등, target은 상호 배타적이라 null 안전 처리).
  - `AdminReportServiceTest`: `Report.ofProduct` 인자를 `mock(Member)`/`mock(Product)`로 교체.

**검증**

- `./gradlew test` **BUILD SUCCESSFUL** — admin 포함 전 도메인 그린 (테스트 컴파일·실행 복구).

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/admin/dto/AdminReportResponse.java` (+3/-3)
- `backend/src/main/java/com/dongnemarket/global/common/event/FavoriteAddedEvent.java` (+8/-0)
- `backend/src/main/java/com/dongnemarket/global/common/event/FavoriteRemovedEvent.java` (+8/-0)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminReportServiceTest.java` (+4/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 이벤트 **발행(FavoriteService)** 및 **리스너·favoriteCount 컬럼(Product, 한상민)** 은 후속 PR B/C. (협업 규약: `docs/design/favorite-comment-count-collaboration.md`)
- Report 파급 수정은 빌드 복구 목적으로 본 PR에 동봉(팀장 협의). 별도 관리 원하시면 분리 가능.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/89
- 이슈: 없음
