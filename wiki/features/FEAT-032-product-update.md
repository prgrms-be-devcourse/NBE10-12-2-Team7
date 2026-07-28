---
id: FEAT-032
type: feature
status: done
author: han95white
date: 2026-06-26
related: []
tags: [global, product, backend, docs]
pr: 32
---

## 무엇을 / 왜

상품 수정 API 구현

## 어떻게 (구현 요약)

- PATCH /api/products/{productId} 상품 수정 API 구현
  - ProductUpdateRequest 추가
  - 작성자 본인만 수정 가능하도록 검증 추가
  - 삭제된 상품 수정 차단
  - 거래완료(COMPLETED) 상품 수정 차단
  - 카테고리 존재 여부 검증
  - 제목 공백, 가격 음수 검증
  - ProductResponse + ApiResponse 응답 유지
  - 상품 수정 Postman 문서 추가

  ## ErrorCode
  - PRODUCT_OWNER_ONLY 기존 코드 사용
  - CANNOT_UPDATE_COMPLETED_PRODUCT 신규 추가

  ## 테스트
  - 상품 수정 성공
  - 미인증 요청 실패
  - 작성자 아님 실패
  - 삭제 상품 수정 실패
  - 거래완료 상품 수정 실패
  - 존재하지 않는 카테고리 실패
  - 제목 공백 실패
  - 가격 음수 실패

  ## 검증
  - sh ./gradlew test --rerun-tasks
  - BUILD SUCCESSFUL

  ## AI 사용 여부
  - AI 사용

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductUpdateRequest.java` (+41/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+16/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+38/-2)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+120/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+132/-0)
- `docs/postman/product-update.md` (+144/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/32
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/30
