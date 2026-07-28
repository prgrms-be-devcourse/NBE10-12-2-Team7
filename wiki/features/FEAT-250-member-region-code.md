---
id: FEAT-250
type: feature
status: done
author: han95white
date: 2026-07-27
related: [FEAT-254]
tags: [admin, chat, favorite, member, trade, backend]
pr: 250
---

## 무엇을 / 왜

회원 동네 regionCode 전환 및 상품 지역 응답 DTO 확장

## 어떻게 (구현 요약)

회원 내 동네 기능을 계층형 지역 마스터 기준으로 전환했습니다.

- 내 동네 설정 요청을 regions에서 regionCodes로 변경
- 신규 동네 설정은 level 3 지역만 허용
- 기존 문자열 region 컬럼은 유지하고 저장 값은 Region.fullName으로 보정
- 응답에 기존 region 유지 + regionCode, regionName, regionFullName 추가
- 기존 회원 동네 문자열 데이터 백필 시더 추가
- chat / favorite / trade / admin 상품 응답 DTO에 지역 코드 필드 추가

**검증**

- RED: member location regionCode 전환 테스트 작성 후 컴파일 실패 확인
- RED: chat / favorite / trade / admin DTO 응답 전환 테스트 작성 후 getter 부재 실패 확인
- GREEN: 구현 후 전체 단위 테스트 통과

테스트 명령
- sh ./gradlew test --rerun-tasks

결과
- BUILD SUCCESSFUL
- 578 tests
- failures 0
- skipped 0

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/admin/dto/AdminProductResponse.java` (+32/-19)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatProductDetail.java` (+38/-25)
- `backend/src/main/java/com/dongnemarket/chat/repository/ChatRoomRepository.java` (+5/-4)
- `backend/src/main/java/com/dongnemarket/favorite/dto/FavoriteProductSummary.java` (+41/-28)
- `backend/src/main/java/com/dongnemarket/member/dto/MemberLocationResponse.java` (+28/-6)
- `backend/src/main/java/com/dongnemarket/member/dto/MemberLocationUpdateRequest.java` (+5/-5)
- `backend/src/main/java/com/dongnemarket/member/entity/MemberLocation.java` (+44/-6)
- `backend/src/main/java/com/dongnemarket/member/init/MemberLocationRegionBackfillSeeder.java` (+57/-0)
- `backend/src/main/java/com/dongnemarket/member/repository/MemberLocationRepository.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/member/service/MemberLocationService.java` (+20/-7)
- `backend/src/main/java/com/dongnemarket/trade/dto/TradeSaleResponse.java` (+37/-24)
- `backend/src/main/resources/db/migration/V4__member_locations_region_fk.sql` (+16/-0)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminProductServiceTest.java` (+21/-10)
- `backend/src/test/java/com/dongnemarket/chat/controller/ChatControllerTest.java` (+25/-7)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+28/-8)
- … 외 5개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

내 동네 설정 API 요청 필드가 변경됩니다.

- 기존: regions
- 변경: regionCodes

신규 설정은 level 3 지역 코드만 허용합니다.
기존 응답 필드 region은 유지되므로 기존 표시 화면은 바로 깨지지 않습니다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/250
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/249
