---
id: FEAT-063
type: feature
status: done
author: han95white
date: 2026-06-29
related: []
tags: [product, backend, docs]
pr: 63
---

## 무엇을 / 왜

내 상품 목록 조회 API 구현

## 어떻게 (구현 요약)

- 내 상품 목록 조회 API를 추가했습니다.
    - `GET /api/members/me/products`
  - 로그인한 사용자가 본인이 등록한 상품 목록만 조회할 수 있도록 구현했습니다.
  - 삭제된 상품은 내 상품 목록에서도 제외했습니다.
  - 숨김 상품은 작성자가 관리할 수 있어야 하므로 내 상품 목록에 포함했습니다.
  - 최신 등록순으로 조회되도록 `id DESC` 정렬을 적용했습니다.
  - 내 상품이 없는 경우 예외가 아니라 `200 OK`와 빈 배열을 반환합니다.
  - 응답은 기존 `ProductSummaryResponse`를 재사용했습니다.
  - Postman 시나리오 문서를 추가했습니다.

  ## 테스트
  - Repository 테스트
    - 본인 상품만 조회
    - 삭제 상품 제외
    - 숨김 상품 포함
    - 최신 등록순 검증
  - Service 테스트
    - 내 상품 목록 응답 변환 검증
    - 상품 없음 시 빈 배열 반환 검증
  - Controller 테스트
    - 인증 없음 시 401
    - 정상 조회 시 200
    - 응답 필드 검증
    - 숨김 상품 포함 및 삭제 상품 제외 검증

  ## 검증 결과

  - 전체 테스트:
    - `sh ./gradlew test --rerun-tasks`
    - 실패 1건: `MemberControllerTest.getMyInfo_invalidToken_returns401`
    - 사유: 테스트 기대값은 `UNAUTHORIZED`, 실제 응답은 `INVALID_TOKEN`
    - 이번 product 작업 범위 밖 기존 실패로 판단했습니다.

  ## 기타
  - 신규 ErrorCode는 추가하지 않았습니다.
  - 변경 범위는 product 패키지와 Postman 문서로 제한했습니다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/product/controller/MyProductController.java` (+31/-0)
- `backend/src/main/java/com/dongnemarket/product/repository/ProductRepository.java` (+3/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+7/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductControllerTest.java` (+85/-15)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+34/-0)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+31/-0)
- `docs/postman/product-my-list.md` (+106/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/63
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/57
