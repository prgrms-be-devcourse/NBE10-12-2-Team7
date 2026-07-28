---
id: FEAT-203
type: feature
status: done
author: jomin4
date: 2026-07-08
related: []
tags: [product, report, backend, infra, docs, refactor]
pr: 203
---

## 무엇을 / 왜

인프라 경계 재정리 — 2축(프로파일×배포지형) + Flyway 도입 (P1~P4)

## 어떻게 (구현 요약)

환경·인프라 세팅을 팀원이 이해하기 쉽게 **2축으로 경계 재정리**했습니다.

- **축① 운영 수위** = Spring 프로파일: `dev` / `prod`
- **축② 배포 지형** = compose 위치: 루트(개발) / `infra/onprem/` / `infra/cloud/`

온프레미스·클라우드는 같은 `prod` 프로파일·같은 앱 이미지·같은 nginx 라우팅을 공유하고, 차이는 env 바인딩(`FILE_STORAGE_TYPE`·`DB_URL`·이미지 출처)으로만 가릅니다.

설계 배경·결정은 ADR로 남겼습니다 → `docs/adr/0004-infra-boundary.md`, `docs/adr/0005-flyway-migration.md`. 현재 구조 전수 목록은 `docs/architecture/07-infra-inventory.md`.

**검증**

- **Flyway**: 빈 DB에 `prod` 부팅 → Flyway가 V1 적용 → Hibernate `validate` 통과 → `Started` 확인.
- **테스트**: `./gradlew test`(H2, Flyway OFF) 통과.
- **dev**: `dev` 프로파일 부팅 정상(DB 접속·`Started`).
- **compose**: 루트/onprem `docker compose config` 파싱·마운트경로 통과. nginx 두 벌 바이트 동일.

## 건드린 파일

- `.env.example` (+9/-14)
- `.github/workflows/cd-app.yml` (+3/-0)
- `.github/workflows/ci.yml` (+14/-0)
- `README.md` (+13/-13)
- `backend/build.gradle` (+5/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductImageStorageService.java` (+1/-1)
- `backend/src/main/java/com/dongnemarket/report/service/EvidenceImageStorageService.java` (+1/-1)
- `backend/src/main/resources/application-demo.yml` (+1/-1)
- `backend/src/main/resources/application-local.yml` (+0/-18)
- `backend/src/main/resources/application-prod.yml` (+24/-0)
- `backend/src/main/resources/application.yml` (+8/-1)
- `backend/src/main/resources/db/migration/V1__baseline.sql` (+333/-0)
- `backend/src/test/resources/application-test.yml` (+4/-0)
- `docker-compose.yml` (+9/-125)
- `docs/README.md` (+1/-1)
- … 외 23개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/203
- 이슈: 없음
