---
id: FEAT-185
type: feature
status: done
author: jomin4
date: 2026-07-07
related: []
tags: [category, global, region, comment, favorite, notification]
pr: 185
---

## 무엇을 / 왜

시더 공통화 - DataSeeder/SeedOrchestrator로 init 통합

## 어떻게 (구현 요약)

- 도메인별로 흩어져 있던 초기화(init) 코드를 `global/init` 아래로 **통합**하고, 데모 데이터를 프로파일로 켜고 끌 수 있게 정리
- `DataSeeder`(Strategy 인터페이스) + `SeedOrchestrator`(단일 진입점, `ApplicationReadyEvent`에서 `order` 오름차순 순차 실행) 도입
- 계층 분리:
  - **master** `CategorySeeder`(10) · `RegionSeeder`(11) — 항상 실행
  - **bootstrap** `AdminSeeder`(20) — `@Profile("!test")`
  - **demo** `DemoDataSeeder`(30) — `@Profile("!test")` + `@ConditionalOnProperty(app.seed.demo=true)`
- `application-demo.yml` 추가: `ddl-auto=create`로 재시작마다 스키마 재생성 → "데모 데이터 한꺼번에 넣다/뺐다"

**사용법**
- 넣기: `./gradlew bootRun --args='--spring.profiles.active=dev,demo'`
- 빼기: `./gradlew bootRun` (마스터만)

**검증**

- [x] 빌드 성공 (`./gradlew test --rerun-tasks` 전체 통과, H2)
- [ ] 담당 API 정상 동작 (Postman 확인) — 해당 없음(엔드포인트 변경 없음)
- [x] develop 최신 반영 (최신 develop에서 분기)
- [x] 공통 응답 형식 준수 — 해당 없음(응답 계층 변경 없음)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/category/init/CategoryInitializer.java` (+0/-40)
- `backend/src/main/java/com/dongnemarket/global/init/DataSeeder.java` (+15/-0)
- `backend/src/main/java/com/dongnemarket/global/init/SeedOrchestrator.java` (+42/-0)
- `backend/src/main/java/com/dongnemarket/global/init/bootstrap/AdminSeeder.java` (+12/-8)
- `backend/src/main/java/com/dongnemarket/global/init/demo/DemoDataSeeder.java` (+26/-18)
- `backend/src/main/java/com/dongnemarket/global/init/master/CategorySeeder.java` (+45/-0)
- `backend/src/main/java/com/dongnemarket/global/init/master/RegionSeeder.java` (+266/-0)
- `backend/src/main/java/com/dongnemarket/region/init/RegionInitializer.java` (+0/-261)
- `backend/src/main/resources/application-demo.yml` (+11/-0)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+1/-1)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+1/-1)
- `backend/src/test/java/com/dongnemarket/global/init/master/CategorySeederTest.java` (+5/-5)
- `backend/src/test/java/com/dongnemarket/global/init/master/RegionSeederTest.java` (+6/-6)
- `backend/src/test/java/com/dongnemarket/notification/controller/NotificationControllerTest.java` (+1/-1)

## 결정과 트레이드오프

- `docs/testing/09-base-init-data.md`의 `app.seed.base-data` → `app.seed.demo` 표기 갱신 필요. 원하면 이 PR에 후속 커밋으로 포함 가능.
- 브랜치명 `feat/baseinidata`(철자: baseinitdata에서 t 하나 누락). 필요 시 정정 가능.

## 남은 이슈 / 후속 작업

- `docs/testing/09-base-init-data.md`의 `app.seed.base-data` → `app.seed.demo` 표기 갱신 필요. 원하면 이 PR에 후속 커밋으로 포함 가능.
- 브랜치명 `feat/baseinidata`(철자: baseinitdata에서 t 하나 누락). 필요 시 정정 가능.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/185
- 이슈: 없음
