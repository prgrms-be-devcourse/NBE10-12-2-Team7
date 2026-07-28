---
id: FEAT-021
type: feature
status: done
author: Crispy-down
date: 2026-06-26
related: []
tags: [favorite, backend, docs]
pr: 21
---

## 무엇을 / 왜

[Favorite] 관심 상품 등록 API 구현

## 어떻게 (구현 요약)

- 관심 상품 등록 API (`POST /api/products/{productId}/favorites`)
- 로그인 사용자 식별(`@AuthenticationPrincipal`), 상품 존재 검증, 중복 검증
- `ApiResponse<FavoriteResponse>` 반환, Swagger 문서화

**검증**

- `FavoriteServiceTest` 4건 (성공 / 상품없음 / 중복 / race condition)
- `FavoriteControllerTest` 4건 (201 / 401 / 404 / 409, 실제 JWT로 `@AuthenticationPrincipal` 바인딩 검증)
- 전체 테스트 그린, `@SpringBootTest` 컨텍스트 정상 기동(H2)
- Postman 수동 테스트 완료 (회원가입 → 상품 등록 → 관심 등록 201)
<img width="588" height="679" alt="image" src="https://github.com/user-attachments/assets/4aaf7418-c433-402b-a94d-2423fa2b3b0e" />
<img width="346" height="601" alt="image" src="https://github.com/user-attachments/assets/d009bf9c-fe1e-4e49-b7ce-2110c06a3367" />
<img width="442" height="580" alt="image" src="https://github.com/user-attachments/assets/7076ecb7-8c6f-4ba9-b9dd-f101914a8be0" />
<img width="440" height="621" alt="image" src="https://github.com/user-attachments/assets/2b3cb22a-a759-4609-97c7-aa431024c58f" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/controller/FavoriteController.java` (+34/-0)
- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+49/-0)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+119/-0)
- `backend/src/test/java/com/dongnemarket/favorite/service/FavoriteServiceTest.java` (+88/-0)
- `docs/postman/favorite-add.md` (+92/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 상품 존재 검증은 `productRepository.existsById` 사용. 현재 소프트삭제(`deleted_at`)·숨김(`hidden`) 상품도 존재로 간주됨.
  삭제/숨김 상품 관심 등록 차단이 필요하면 Product 도메인에 필터 조회 메서드(`existsByIdAndDeletedAtIsNull` 등) 추가 협의 필요.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/21
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/16
