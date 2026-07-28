---
id: FEAT-023
type: feature
status: done
author: han95white
date: 2026-06-26
related: []
tags: [product, backend, docs]
pr: 23
---

## 무엇을 / 왜

상품 목록 조회 API 구현

## 어떻게 (구현 요약)

- GET /api/products 상품 목록 조회 API 구현
  - ProductSummaryResponse 추가
  - 삭제되지 않고 숨김 처리되지 않은 상품만 조회
  - 최신 등록순(id DESC) 정렬 적용
  - 목록 응답에서 description 제외
  - Product hide / softDelete 도메인 메서드 추가
  - Service / Controller 테스트 작성
  - Postman 시나리오 문서 작성

  ## 테스트 결과 & 정상작동 여부
  - `sh ./gradlew test --rerun-tasks`
  - BUILD SUCCESSFUL

  ## 확인 필요
  - 상품 상세 조회는 이번 PR 범위가 아닙니다.
  - 검색/필터/카테고리별 조회는 별도 작업입니다.

  ## AI 사용 여부
  - [x] AI 에이전트를 사용했습니다.
  - [x] AI 생성 코드를 직접 검토했습니다.
  - [x] 담당 패키지 외 파일을 수정하지 않았습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSummaryResponse.java` (+81/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+8/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+5/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+10/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+46/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+19/-0)
- `docs/postman/product-list.md` (+71/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/23
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/19
