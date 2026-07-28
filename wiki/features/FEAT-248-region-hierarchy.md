---
id: FEAT-248
type: feature
status: done
author: han95white
date: 2026-07-27
related: [FEAT-254]
tags: [global, product, region, notification, backend]
pr: 248
---

## 무엇을 / 왜

계층형 지역 마스터 및 상품 regionCode 연동

## 어떻게 (구현 요약)

문자열 지역 기반 구조를 시-구-동 계층형 Region 마스터 기반으로 전환하기 위한 토대를 추가했습니다.

1단계 지역 마스터 도입, 2단계 상품 등록/수정 연동, 3단계 상품 목록·검색 필터 전환을 하나의 흐름으로 묶었습니다. 프론트의 계단식 지역 선택 UI는 지역 조회 API와 상품 등록/수정 regionCode 계약, 상품 목록/검색 regionCode 필터를 함께 필요로 하므로 한 PR에서 같이 리뷰하는 것이 자연스럽다고 판단했습니다.

**검증**

```text
backend/src/test/java/com/dongnemarket/notification/service/NotificationPriceChangeTest.java
```

사유: 해당 테스트가 ProductService.updateProduct()를 직접 호출하고 있어, 상품 수정 요청 계약이 regionCode 필수로 바뀐 영향을 받았습니다.

변경 내용: 테스트용 Region 계층 데이터를 만들고 ProductUpdateRequest에 regionCode를 넣도록 테스트 준비 데이터만 보정했습니다.

기능 로직 변경은 없습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/init/master/RegionSeeder.java` (+141/-250)
- `backend/src/main/java/com/dongnemarket/product/controller/ProductController.java` (+4/-4)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductCreateRequest.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductResponse.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSearchRequest.java` (+5/-5)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSummaryResponse.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductUpdateRequest.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+48/-2)
- `backend/src/main/java/com/dongnemarket/product/init/ProductRegionBackfillSeeder.java` (+52/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/spec/ProductSpecification.java` (+30/-9)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+27/-20)
- `backend/src/main/java/com/dongnemarket/region/controller/RegionController.java` (+4/-2)
- `backend/src/main/java/com/dongnemarket/region/dto/RegionResponse.java` (+42/-2)
- `backend/src/main/java/com/dongnemarket/region/entity/Region.java` (+97/-2)
- … 외 12개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 상품 등록/수정 요청에서 regionCode가 필수입니다.
- 신규 등록/수정은 level 3 지역만 허용합니다.
- 상품 목록/검색의 기존 regions 파라미터를 제거했습니다.
- 상품 목록/검색은 regionCodes 파라미터를 사용해야 합니다.
- 프론트는 상품 등록/수정/목록/검색에서 계단식 지역 선택 UI와 regionCode 전달로 전환해야 합니다.
- 응답 필드 추가(regionCode, regionName, regionFullName)는 기존 region을 유지하므로 하위 호환입니다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/248
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/247
