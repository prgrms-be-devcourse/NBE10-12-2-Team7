---
id: FEAT-184
type: feature
status: done
author: jomin4
date: 2026-07-07
related: []
tags: [product, report, support, backend, docs]
pr: 184
---

## 무엇을 / 왜

전체 프로젝트를 실측해 docs에 미반영된 부분(클라우드 배포·CI/CD 등)을 보강하고, Testcontainers 통합 테스트를 제거한다.

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- 문서 — `docs/` 1개, `docs/adr/` 3개, `docs/architecture/` 3개, `docs/conventions/` 2개, `docs/runbook/` 3개
- 인프라·CI — `.github/` 1개, `.github/workflows/` 1개
- 테스트 — 8개 파일 (+0줄)

**검증**

- `build.gradle`: testcontainers 의존성·`integrationTest` 태스크 제거, `test`는 `useJUnitPlatform()`
- 통합 테스트 8개 파일 삭제(테스트 4 + 통합 설정/시나리오 SQL 4)
- 개발 흐름 5단계 복귀(단위 테스트 → API 테스트 → PR)
- 관련 문서(testing·ci-cd·PR템플릿·git-collaboration) 반영
- 로컬 `./gradlew compileTestJava` 통과 확인

## 건드린 파일

- `.github/pull_request_template.md` (+1/-2)
- `.github/workflows/ci.yml` (+1/-1)
- `AGENTS.md` (+4/-3)
- `README.md` (+11/-4)
- `backend/build.gradle` (+2/-19)
- `backend/src/test/java/com/dongnemarket/product/integration/ProductFavoriteCountHandlerIntegrationTest.java` (+0/-121)
- `backend/src/test/java/com/dongnemarket/report/integration/ReportConcurrencyIntegrationTest.java` (+0/-106)
- `backend/src/test/java/com/dongnemarket/report/integration/ReportControllerIntegrationTest.java` (+0/-336)
- `backend/src/test/java/com/dongnemarket/support/BaseIntegrationTest.java` (+0/-34)
- `backend/src/test/resources/application-integration.yml` (+0/-25)
- `backend/src/test/resources/sql/clean-scenario.sql` (+0/-8)
- `backend/src/test/resources/sql/products-scenario.sql` (+0/-9)
- `backend/src/test/resources/sql/report-scenario.sql` (+0/-17)
- `docs/README.md` (+3/-3)
- `docs/adr/0002-db-hosting-ec2-mysql.md` (+29/-0)
- … 외 10개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/184
- 이슈: 없음
