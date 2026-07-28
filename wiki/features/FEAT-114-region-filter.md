---
id: FEAT-114
type: feature
status: done
author: han95white
date: 2026-07-03
related: []
tags: [global, product, region, backend]
pr: 114
---

## 무엇을 / 왜

지역 마스터 및 상품 지역 필터 추가

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `global` 도메인 — SecurityConfig(기타, 신규)
- `product` 도메인 — ProductController(컨트롤러, 수정), ProductSearchRequest(DTO, 신규), Product(엔티티, 수정), ProductSpecification(리포지토리, 수정), ProductService(서비스, 수정)
- `region` 도메인 — RegionController(컨트롤러, 신규), RegionResponse(DTO, 신규), Region(엔티티, 신규), RegionInitializer(시더, 신규), RegionRepository(리포지토리, 신규), RegionService(서비스, 신규)
- 추가된 API 표면 — `@RequestMapping("/api/regions")`, `@GetMapping`
- 테스트 — 6개 파일 (+570줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+6/-4)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSearchRequest.java` (+15/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+2/-1)
- `backend/src/main/java/com/dongnemarket/product/repository/spec/ProductSpecification.java` (+24/-4)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+46/-4)
- `backend/src/main/java/com/dongnemarket/region/controller/RegionController.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/region/dto/RegionResponse.java` (+26/-0)
- `backend/src/main/java/com/dongnemarket/region/entity/Region.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/region/init/RegionInitializer.java` (+57/-0)
- `backend/src/main/java/com/dongnemarket/region/repository/RegionRepository.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/region/service/RegionService.java` (+26/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+120/-0)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+171/-2)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+111/-5)
- … 외 3개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/114
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/110
