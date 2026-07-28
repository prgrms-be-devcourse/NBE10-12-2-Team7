---
id: FEAT-162
type: feature
status: done
author: han95white
date: 2026-07-06
related: []
tags: [product, admin, backend]
pr: 162
---

## 무엇을 / 왜

공개 상품 조회 노출 정책 적용

## 어떻게 (구현 요약)

- 일반 사용자 공개 상품 조회에서 탈퇴·정지 판매자 상품을 제외했습니다.
  - 일반 사용자 공개 상품 조회에서 거래완료 상품을 제외했습니다.
  - 상품 상세 직접 접근도 동일하게 막았습니다.
    - 탈퇴·정지 판매자 상품: PRODUCT_NOT_FOUND
    - 거래완료 상품: PRODUCT_NOT_FOUND
  - 카테고리별 상품 목록도 동일한 공개 노출 정책을 타도록 Specification 기반 조회로 전환했습니다.
  - 관리자 상품 조회는 기존처럼 전체 상품을 볼 수 있도록 유지했습니다. 코드 변경 없이 테스트로 보장했습니다.

  ## 정책

  공개 조회 노출 조건:

  ```text
  deletedAt IS NULL
  hidden = false
  tradeStatus != COMPLETED
  member.status = ACTIVE
  ```

  적용 대상:

  GET /api/products
  GET /api/products/search
  GET /api/categories/{categoryId}/products
  GET /api/products/{productId}

  관리자 조회는 기존처럼 제외 필터 없이 전체 상품을 조회합니다.

  GET /api/admin/products
  GET /api/admin/products/{productId}

  ## 테스트

  RED:

  - ProductSpecification.categoryList 미구현으로 컴파일 실패 확인

  GREEN:

  sh ./gradlew test --tests 'com.dongnemarket.product.repository.ProductRepositoryTest' --tests 'com.dongnemarket.product.service.ProductServiceTest' --tests
  'com.dongnemarket.product.controller.ProductControllerTest' --tests 'com.dongnemarket.admin.service.AdminProductServiceTest'

  결과: BUILD SUCCESSFUL

  전체 단위 테스트:

  sh ./gradlew test

  결과:

  471 tests / failures 0 / errors 0 / skipped 0
  BUILD SUCCESSFUL

  ## 범위 밖

  - 상품 데이터 자체는 변경하지 않았습니다.
      - deletedAt 변경 없음
      - hidden 변경 없음
      - tradeStatus 변경 없음

  - 관리자 상품 조회 코드는 변경하지 않았습니다.
  - favorite/comment 접근 정책은 이번 범위에서 변경하지 않았습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+0/-3)
- `backend/src/main/java/com/dongnemarket/product/repository/spec/ProductSpecification.java` (+18/-2)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+11/-1)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminProductServiceTest.java` (+51/-4)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+160/-2)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+171/-2)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+80/-29)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/162
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/161
