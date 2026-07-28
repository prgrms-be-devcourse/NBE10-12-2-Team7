---
id: FEAT-100
type: feature
status: done
author: han95white
date: 2026-07-03
related: [FEAT-120]
tags: [product, backend]
pr: 100
---

## 무엇을 / 왜

상품 이미지 등록 및 대표이미지 설정 구현

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `product` 도메인 — ProductController(컨트롤러, 수정), ProductCreateRequest(DTO, 신규), ProductResponse(DTO, 수정), ProductSummaryResponse(DTO, 수정), ProductUpdateRequest(DTO, 신규), Product(엔티티, 신규), ProductImage(엔티티, 신규), ProductImageRepository(리포지토리, 신규), ProductService(서비스, 수정)
- 테스트 — 3개 파일 (+431줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+3/-2)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductCreateRequest.java` (+24/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductResponse.java` (+20/-1)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSummaryResponse.java` (+8/-1)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductUpdateRequest.java` (+24/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/ProductImage.java` (+69/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductImageRepository.java` (+19/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+48/-4)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+55/-8)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductImageRepositoryTest.java` (+85/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+291/-2)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/100
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/98
