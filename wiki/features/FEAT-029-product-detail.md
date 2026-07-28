---
id: FEAT-029
type: feature
status: done
author: han95white
date: 2026-06-26
related: []
tags: [product, backend, docs]
pr: 29
---

## 무엇을 / 왜

상품 상세 조회 API 구현

## 어떻게 (구현 요약)

- GET /api/products/{productId} 상품 상세 조회 API 구현
  - 상품 상세 조회 시 viewCount 1 증가
  - ProductResponse 상세 응답 반환(description 포함)
  - 존재하지 않는 상품 / 삭제 상품 / 숨김 상품 예외 처리
  - Product 도메인 메서드 추가
    - increaseViewCount()
    - isDeleted()
  - Service / Controller 테스트 작성
  - Postman 시나리오 문서 작성

  ## 테스트 결과 & 정상작동 여부
  - `sh ./gradlew test --rerun-tasks`
  - BUILD SUCCESSFUL

  ## 확인 필요
  - 상품 수정/삭제/거래상태 변경은 이번 PR 범위가 아닙니다.
  - 숨김 상품은 일반 상세 조회에서 403 HIDDEN_PRODUCT로 처리했습니다.

  ## AI 사용 여부
  - [x] AI 에이전트를 사용했습니다.
  - [x] AI 생성 코드를 직접 검토했습니다.
  - [x] 담당 패키지 외 파일을 수정하지 않았습니다.

  현재 로컬에는 .DS_Store, docs/.DS_Store만 untracked로 남아 있습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+7/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+8/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+15/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+65/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+54/-0)
- `docs/postman/product-detail.md` (+86/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/29
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/25
