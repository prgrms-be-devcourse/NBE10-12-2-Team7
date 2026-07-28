---
id: FEAT-035
type: feature
status: done
author: han95white
date: 2026-06-26
related: []
tags: [product, backend, docs]
pr: 35
---

## 무엇을 / 왜

상품 삭제 API 구현

## 어떻게 (구현 요약)

- DELETE /api/products/{productId} 상품 삭제 API 구현
  - 상품 물리 삭제가 아닌 deletedAt 기록 방식의 논리 삭제 처리
  - 작성자 본인만 삭제 가능하도록 권한 검증 추가
  - 이미 삭제된 상품 재삭제 차단
  - 거래완료(COMPLETED) 상품도 작성자라면 삭제 가능하도록 정책 반영
  - 상품 삭제 Postman 시나리오 문서 추가

  ## ErrorCode
  - 신규 ErrorCode 없음
  - 기존 PRODUCT_NOT_FOUND 사용
  - 기존 DELETED_PRODUCT 사용
  - 기존 PRODUCT_OWNER_ONLY 사용

  ## 테스트
  - 작성자 상품 삭제 성공
  - 거래완료 상품 삭제 성공
  - 존재하지 않는 상품 삭제 시 PRODUCT_NOT_FOUND
  - 이미 삭제된 상품 재삭제 시 DELETED_PRODUCT
  - 작성자가 아닌 사용자 삭제 시 PRODUCT_OWNER_ONLY
  - 인증 없이 삭제 요청 시 UNAUTHORIZED

  ## 검증
  - sh ./gradlew test --rerun-tasks
  - BUILD SUCCESSFUL

  ## AI 사용 여부
  - AI 사용

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+14/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+56/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+66/-0)
- `docs/postman/product-delete.md` (+131/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/35
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/33
