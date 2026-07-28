---
id: FEAT-143
type: feature
status: done
author: han95white
date: 2026-07-06
related: []
tags: [product, products, backend, frontend]
pr: 143
---

## 무엇을 / 왜

상품 목록 커서 페이지네이션 구현

## 어떻게 (구현 요약)

상품 목록 조회 API에 커서 기반 페이지네이션을 적용한다.

  기존: `GET /api/products` 응답 data가 상품 배열
  변경: `GET /api/products` 응답 data가 `{ items, nextCursor, hasNext }` 페이지 객체

  ## 변경 이유

  상품 목록은 데이터가 계속 늘어나는 조회 API라 offset 방식보다 커서 방식이 적합하다.
  최신순 목록 기준으로 `id DESC`, 다음 페이지는 `id < cursor` 조건을 사용한다.

  ## 변경 파일

  - `ProductPageResponse` — 상품 목록 페이지 응답 DTO 추가
  - `ProductController` — `cursor`, `size` 파라미터 추가 및 목록 응답 타입 변경
  - `ProductService` — size clamp, size+1 조회, hasNext/nextCursor 계산
  - `ProductSpecification` — 목록 조건에 cursor 조건 추가
  - `ProductControllerTest` — 목록 응답 구조 및 커서 동작 검증
  - `ProductServiceTest` — 페이지 응답 생성 로직 검증
  - `ProductRepositoryTest` — cursor 조건과 regions 조건 조합 검증

  ## 정책

  - 정렬: 최신순 `id DESC`
  - 다음 페이지 조건: `id < cursor`
  - `size + 1` 조회로 `hasNext` 판정
  - count 쿼리 사용 없음
  - 기본 size: 30
  - 최대 size: 100
  - 마지막 페이지는 `hasNext=false`, `nextCursor=null`
  - 기존 `regions` 필터는 cursor 조건과 함께 동작

  ## 범위 밖

  - 상품 검색
  - 내 상품 목록
  - 카테고리별 상품 목록
  - 관리자 상품 목록
  - 관심/신고 목록

  ## 테스트

  - product Controller/Service/Repository 테스트 통과
  - 전체 단위 테스트 통과

  ```bash
  sh ./gradlew test

  결과:

  BUILD SUCCESSFUL
  tests: 391
  failures: 0
  errors: 0
  skipped: 0

  ## 주의 사항

  GET /api/products의 응답 구조가 변경된다.

  기존: data
  변경: data.items, data.nextCursor, data.hasNext
  
  
    ## 추가 수정

  프론트 상품 목록 페이지가 `GET /api/products`의 커서 페이지 응답을 소비하도록 수정했습니다.

  - 기존 배열 응답 처리 제거
  - `data.items`, `data.nextCursor`, `data.hasNext` 기준으로 처리
  - 첫 조회/지역 변경 시 첫 페이지를 새로 조회
  - `더 보기` 클릭 시 `cursor=nextCursor&size=30`으로 다음 페이지 조회
  - 다음 페이지 결과는 기존 목록 뒤에 append
  - `hasNext=false`면 더 보기 버튼 숨김
  - 더 보기 중복 클릭 방지

  검색/카테고리/정렬은 기존처럼 현재까지 로드된 상품 목록 기준으로 유지했습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+6/-3)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductPageResponse.java` (+32/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/spec/ProductSpecification.java` (+15/-1)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+34/-11)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+176/-25)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+104/-18)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+99/-31)
- `frontend/src/app/products/page.module.css` (+30/-0)
- `frontend/src/app/products/page.tsx` (+114/-51)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/143
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/142
