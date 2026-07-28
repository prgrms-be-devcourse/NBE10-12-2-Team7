---
id: FEAT-135
type: feature
status: done
author: han95white
date: 2026-07-06
related: []
tags: [region, backend]
pr: 135
---

## 무엇을 / 왜

지역 마스터 시드 전국 시군구 확장

## 어떻게 (구현 요약)

지역 마스터 초기 시드를 기존 서울 25개 구에서 전국 시군구 기준으로 확장했습니다.

  - 기존: 서울 25개 구
  - 변경: 전국 시군구 229개
  - 형식: `광역명 + 공백 하나 + 시/군/구`
  - 예시: `서울 강남구`, `부산 해운대구`, `경기 성남시`, `제주 제주시`
  - 세종은 하위 구분 없이 `세종` 단일 값으로 저장

  ## 변경 내용

  - `RegionInitializer`
    - 기본 지역 시드 목록을 전국 시군구 229개로 확장
    - 기존 항목별 `existsByName` 가드 방식은 유지

  - `RegionInitializerTest`
    - 서울 25개 고정 개수 단언 제거
    - 시드 결과가 비어 있지 않은지 검증
    - 대표 지역들이 포함되는지 검증
    - 초기화기 재실행 시 중복 저장되지 않는지 검증
    - 일부 지역만 있을 때 누락 지역이 보충되는지 검증

  ## 범위 밖

  - Region 엔티티/Repository/Service/Controller 구조 변경 없음
  - `GET /api/regions` API 스펙 변경 없음
  - 지역 추가/수정/삭제 API 추가 없음
  - 좌표, 동 단위, 계층 필드 추가 없음
  - product/member/global 등 타 도메인 수정 없음

  ## 테스트

  ```bash
  sh ./gradlew test

  결과:

  BUILD SUCCESSFUL
  tests: 363
  failures: 0
  errors: 0
  skipped: 0

  ## 참고 사항

  이번 작업은 지역 마스터 기준 데이터를 확장하는 작업이며, 런타임에서 지역을 추가하거나 수정하는 기능은 포함하지 않았습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/region/init/RegionInitializer.java` (+205/-1)
- `backend/src/test/java/com/dongnemarket/region/init/RegionInitializerTest.java` (+31/-31)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/135
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/134
