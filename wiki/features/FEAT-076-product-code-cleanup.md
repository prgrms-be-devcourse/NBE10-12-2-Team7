---
id: FEAT-076
type: feature
status: done
author: han95white
date: 2026-07-01
related: []
tags: [admin, product, category, comment, favorite, report]
pr: 76
---

## 무엇을 / 왜

price 타입 BigDecimal 전환

## 어떻게 (구현 요약)

- Product 가격 타입을 Integer에서 BigDecimal로 전환했습니다.
- Product 엔티티, 생성자, 정적 팩토리, update 메서드, getter를 BigDecimal 기준으로 수정했습니다.
- 상품 등록/수정/검색 요청 DTO와 상품 응답 DTO의 가격 타입을 BigDecimal로 맞췄습니다.
- ProductController의 검색 가격 파라미터(minPrice, maxPrice)를 BigDecimal로 변경했습니다.
- ProductService의 가격 검증 로직을 signum(), compareTo() 기준으로 변경했습니다.
- ProductSpecification의 가격 조건 검색을 BigDecimal 기준으로 변경했습니다.
- 기존 Product 엔티티의 불필요한 주석과 unused import도 함께 정리했습니다.

**검증**

- sh ./gradlew test --tests 'com.dongnemarket.product.*'
  - BUILD SUCCESSFUL
- sh ./gradlew test
  - BUILD SUCCESSFUL
- API 경로, ErrorCode, 응답 구조는 변경하지 않았습니다.
- JSON 요청의 price 값은 기존처럼 숫자로 전송 가능합니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/admin/dto/AdminProductResponse.java` (+5/-4)
- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+3/-2)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductCreateRequest.java` (+6/-3)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductResponse.java` (+5/-3)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSearchRequest.java` (+8/-5)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSummaryResponse.java` (+5/-3)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductUpdateRequest.java` (+6/-3)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+7/-6)
- `backend/src/main/java/com/dongnemarket/product/repository/spec/ProductSpecification.java` (+7/-4)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+7/-6)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminProductControllerTest.java` (+7/-5)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminProductServiceTest.java` (+9/-7)
- `backend/src/test/java/com/dongnemarket/category/controller/CategoryControllerTest.java` (+7/-5)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+3/-1)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+4/-2)
- … 외 5개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 타 담당 영역 연결 수정 사항:
  - AdminProductResponse의 price 타입을 BigDecimal로 변경했습니다.
  - AdminProductControllerTest, AdminProductServiceTest의 Product 테스트 데이터 가격을 BigDecimal.valueOf(...) 기준으로 변경했습니다.
  - CategoryControllerTest의 Product 테스트 데이터 가격을 BigDecimal.valueOf(...) 기준으로 변경했습니다.
  - FavoriteControllerTest의 Product 테스트 데이터 가격을 BigDecimal.valueOf(...) 기준으로 변경했습니다.
  - CommentControllerTest의 Product 테스트 데이터 가격을 BigDecimal.valueOf(...) 기준으로 변경했습니다.
  - ReportServiceTest의 Product 테스트 데이터 가격을 BigDecimal.valueOf(...) 기준으로 변경했습니다.
- 타 도메인 서비스 로직은 변경하지 않았고, Product price 타입 전환에 따른 DTO/테스트 연결부만 수정했습니다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/76
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/78
