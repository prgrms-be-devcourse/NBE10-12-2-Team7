---
id: FEAT-242
type: feature
status: done
author: Crispy-down
date: 2026-07-27
related: [FEAT-233]
tags: [favorite, global, backend]
pr: 242
---

## 무엇을 / 왜

자기 상품 관심(찜) 등록 차단

## 어떻게 (구현 요약)

- 판매자가 **자기 상품을 관심(찜) 등록할 수 없도록** 차단. 자기 상품 채팅(`CANNOT_CHAT_WITH_SELF`)·신고(`CANNOT_REPORT_OWN_PRODUCT`) 차단과 일관성을 맞춤
- ErrorCode `CANNOT_FAVORITE_OWN_PRODUCT`(400, `FAVORITE_003`) 추가
- `FavoriteService.add`: 접근성 검증 직후 소유자 검증(`product.getMember().getId().equals(memberId)`, `ChatRoomCreator` 패턴 대칭 — LAZY 프록시 id 접근이라 추가 쿼리 없음)
- `validateFavoriteCreatable`가 로드한 `Product`를 반환해 `add()`가 재조회 없이 재사용 (검증 순서: 접근성 → 자기소유 → 중복)

**검증**

- `FavoriteControllerTest`: 판매자가 자기 상품 등록 시 400 `CANNOT_FAVORITE_OWN_PRODUCT` 케이스 추가
- `FavoriteServiceTest`: 자기 소유 상품 차단 단위 테스트 추가 (저장·이벤트 미발행 검증)
- favorite·notification·product 스위트 전체 그린 (기존 성공/중복(409) 등 회귀 없음)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+11/-5)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+1/-0)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+11/-0)
- `backend/src/test/java/com/dongnemarket/favorite/service/FavoriteServiceTest.java` (+31/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **BE only PR입니다.** FE(상품 상세 찜 버튼 `!isOwner` 가드)는 별도 작업(유진님). BE만 머지되면 판매자가 하트 클릭 시 400이 노출되는 과도기가 있음 → FE 가드와 함께 배포되면 해소됨
- 신규 ErrorCode 1개(`FAVORITE_003`)
- 기존 `favorite` 쿼리 레벨 판매자 제외(3차-A, PR #233)와 belt-and-suspenders 관계 유지 — 레거시 자기찜 row 대비 쿼리 제외는 그대로 둠

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/242
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/241
