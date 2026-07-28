---
id: FEAT-256
type: feature
status: done
author: han95white
date: 2026-07-28
related: []
tags: [admin, chat, favorite, global, member, product]
pr: 256
---

## 무엇을 / 왜

지역 문자열 호환 코드 제거

## 어떻게 (구현 요약)

지역 계층화 전환 이후 남아 있던 문자열 지역 호환 코드를 제거했습니다.

- Product / MemberLocation의 옛 문자열 `region` 필드 제거
- 상품·동네·chat·trade·favorite·admin 응답 DTO의 옛 `region` 필드 제거
- `Region.name` 및 이름 기반 호환 조회 코드 제거
- product/member 지역 백필 시더 및 테스트 제거
- V5 Flyway 마이그레이션 추가: `products.region`, `member_locations.region`, `regions.name` 제거
- 지역 구조 변경에 맞춰 API/ERD/DB 마이그레이션 문서 갱신
- `.DS_Store` 무시 설정 추가

**검증**

- `sh ./gradlew test`
- 결과: `BUILD SUCCESSFUL`

## 건드린 파일

- `.gitignore` (+1/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminProductResponse.java` (+19/-23)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatProductDetail.java` (+23/-27)
- `backend/src/main/java/com/dongnemarket/favorite/dto/FavoriteProductSummary.java` (+20/-24)
- `backend/src/main/java/com/dongnemarket/global/init/demo/DemoDataSeeder.java` (+22/-6)
- `backend/src/main/java/com/dongnemarket/global/init/master/RegionSeeder.java` (+2/-35)
- `backend/src/main/java/com/dongnemarket/member/dto/MemberLocationResponse.java` (+5/-12)
- `backend/src/main/java/com/dongnemarket/member/entity/MemberLocation.java` (+1/-25)
- `backend/src/main/java/com/dongnemarket/member/init/MemberLocationRegionBackfillSeeder.java` (+0/-57)
- `backend/src/main/java/com/dongnemarket/member/repository/MemberLocationRepository.java` (+0/-2)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductCreateRequest.java` (+2/-17)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductResponse.java` (+7/-14)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductSummaryResponse.java` (+7/-14)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductUpdateRequest.java` (+2/-17)
- `backend/src/main/java/com/dongnemarket/product/entity/Product.java` (+4/-37)
- … 외 32개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/256
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/255
