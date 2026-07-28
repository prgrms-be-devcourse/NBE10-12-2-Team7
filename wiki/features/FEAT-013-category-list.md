---
id: FEAT-013
type: feature
status: done
author: han95white
date: 2026-06-25
related: []
tags: [category, backend, docs]
pr: 13
---

## 무엇을 / 왜

카테고리 목록 조회 API 구현

## 어떻게 (구현 요약)

- Category Entity / Repository 추가
  - 기본 카테고리 8개 초기화 로직 추가
  - GET /api/categories 카테고리 목록 조회 API 구현
  - CategoryResponse / Service / Controller 추가
  - 카테고리 초기화 및 목록 조회 테스트 작성
  - Postman 시나리오 문서 정리

  ## 테스트 결과 & 정상작동 여부
  - `sh ./gradlew test --rerun-tasks`
  - BUILD SUCCESSFUL

  ## 확인 필요
  - 카테고리 추가/수정/삭제 API는 이번 PR 범위가 아닙니다.
  - 특정 카테고리 상품 조회 API는 별도 작업입니다.

  ## AI 사용 여부
  - [x] AI 에이전트를 사용했습니다.
  - [x] AI 생성 코드를 직접 검토했습니다.
  - [x] 담당 패키지 외 파일을 수정하지 않았습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/category/controller/CategoryController.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/category/dto/CategoryResponse.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/category/entity/Category.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/category/init/CategoryInitializer.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/category/repository/CategoryRepository.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/category/service/CategoryService.java` (+26/-0)
- `backend/src/test/java/com/dongnemarket/category/controller/CategoryControllerTest.java` (+36/-0)
- `backend/src/test/java/com/dongnemarket/category/init/CategoryInitializerTest.java` (+54/-0)
- `docs/dev-log/2026-06-25-category-base.md` (+86/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/13
- 이슈: 없음
