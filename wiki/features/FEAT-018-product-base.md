---
id: FEAT-018
type: feature
status: done
author: han95white
date: 2026-06-26
related: []
tags: [global, product, backend, docs]
pr: 18
---

## 무엇을 / 왜

상품 등록 API 구현

## 어떻게 (구현 요약)

- Product Entity / TradeStatus / ProductRepository 추가
  - POST /api/products 상품 등록 API 구현
  - ProductCreateRequest / ProductResponse DTO 추가
  - 로그인 사용자 기반 상품 등록 처리
  - 카테고리 존재 여부 검증
  - 상품 제목/가격 도메인 검증 추가
  - Product Service / Controller / Repository 테스트 작성
  - Postman 시나리오 문서 작성

  ## 테스트 결과 & 정상작동 여부
  - `sh ./gradlew test --rerun-tasks`
  - BUILD SUCCESSFUL

  ## 확인 필요
  - 상품 목록/상세/수정/삭제/거래상태 변경은 이번 PR 범위가 아닙니다.
  - 가격 타입은 설계 기준에 맞춰 `Integer`로 구현했습니다.

  ## AI 사용 여부
  - [x] AI 에이전트를 사용했습니다.
  - [x] AI 생성 코드를 직접 검토했습니다.
  - [x] 담당 패키지 외 파일을 수정하지 않았습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+37/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductCreateRequest.java` (+41/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductResponse.java` (+87/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+125/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/TradeStatus.java` (+7/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+7/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+61/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+174/-0)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+66/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+135/-0)
- `docs/postman/product-create.md` (+150/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/18
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/17
