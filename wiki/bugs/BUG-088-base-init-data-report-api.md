---
id: BUG-088
type: bug
status: done
author: jomin4
date: 2026-07-01
related: [FEAT-085, FEAT-087]
tags: [admin, global, backend]
pr: 88
---

## 증상

신고 리팩터로 깨진 develop 빌드 복구 (긴급) — 상세 증상 미기록.

## 원인

`Report`의 `Long` id 게터/팩토리(`getReporterId()`, `ofProduct(Long,Long,…)` 등)가 엔티티 기반으로 교체됨. 아래 두 파일이 미갱신 상태였음(서로 다른 파일이라 git 머지가 못 잡는 의미론적 충돌):

## 해결 방법

신고 리팩터(#85)가 `Report`를 `@ManyToOne`(Member/Product) 매핑으로 바꾸면서, 옛 API를 쓰던 두 파일이 컴파일 오류를 냅니다. 현재 **develop이 빌드되지 않아 전원 작업이 막힌 상태**라 즉시 복구합니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/admin/dto/AdminReportResponse.java` (+3/-3)
- `backend/src/main/java/com/dongnemarket/global/init/BaseInitDataInitializer.java` (+5/-5)

## 재발 방지

`./gradlew compileJava` → **BUILD SUCCESSFUL** 확인.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/88
- 이슈: 없음
