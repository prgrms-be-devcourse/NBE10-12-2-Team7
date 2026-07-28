---
id: FEAT-176
type: feature
status: done
author: han95white
date: 2026-07-07
related: [FEAT-182]
tags: [product, backend]
pr: 176
---

## 무엇을 / 왜

상품 이미지 업로드 API 추가

## 어떻게 (구현 요약)

- 상품 이미지 업로드 API를 추가했습니다.
  - 업로드된 상품 이미지를 조회하는 API를 추가했습니다.
  - 상품 등록/수정 API는 기존처럼 imageUrls를 받는 구조를 유지했습니다.
  - 회원이 이미지 URL을 직접 준비하지 않고, 업로드 API 응답 URL을 상품 등록/수정 요청에 사용할 수 있도록 했습니다.

  ## API

  ```http
  POST /api/products/images
  Content-Type: multipart/form-data
  Authorization: Bearer {token}
  ```  

  요청 필드:

  files

  응답 예시:

  {
    "status": 201,
    "message": "상품 이미지가 업로드되었습니다.",
    "data": {
      "imageUrls": [
        "/api/products/images/{filename}"
      ]
    }
  }

  이미지 조회:

  GET /api/products/images/{filename}

  ## 검증 정책

  - 최소 1장, 최대 5장
  - 파일당 최대 5MB
  - 허용 타입: image/jpeg, image/png, image/gif, image/webp
  - 잘못된 파일은 INVALID_INPUT_VALUE
  - 없는 이미지 조회는 PRODUCT_NOT_FOUND

  ## 테스트

  RED:

  - ProductImageStorageService / ProductImageController / ProductImageUploadResponse 미존재로 compile 실패 확인

  GREEN:

  sh ./gradlew test --tests 'com.dongnemarket.product.service.ProductImageStorageServiceTest' --tests 'com.dongnemarket.product.controller.ProductImageControllerTest'

  결과: BUILD SUCCESSFUL

  전체 단위 테스트:

  sh ./gradlew test

  결과:

  500 tests / failures 0 / errors 0 / skipped 0
  BUILD SUCCESSFUL

  ## 범위 밖

  - SecurityConfig 수정 없음
  - ErrorCode 신규 추가 없음
  - application.yml 수정 없음
  - report 이미지 업로드와 공통화 없음
  - S3/외부 스토리지 연동 없음
  - 업로드 후 미사용 파일 정리 없음

  ## 팀 공유 사항

  현재 product 영역 밖을 건드리지 않기 위해 SecurityConfig는 수정하지 않았습니다.

  상품 이미지를 비로그인 사용자도 조회해야 한다면 후속으로 아래 허용이 필요합니다.

  GET /api/products/images/** permitAll

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductImageController.java` (+53/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductImageUploadResponse.java` (+20/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductImageStorageService.java` (+102/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductImageControllerTest.java` (+170/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductImageStorageServiceTest.java` (+138/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/176
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/175
