---
id: FEAT-039
type: feature
status: done
author: Crispy-down
date: 2026-06-26
related: []
tags: [favorite, backend, docs]
pr: 39
---

## 무엇을 / 왜

[Favorite] 관심 상품 취소 API 구현

## 어떻게 (구현 요약)

- 관심 상품 취소 API (`DELETE /api/products/{productId}/favorites`)
- 본인이 등록한 관심 상품(`member_id` + `product_id`)을 조회 후 삭제
- `ApiResponse<Void>` 반환(200, data 없음), Swagger 문서화

**검증**

- `FavoriteServiceTest` 취소 2건 추가 (성공 / 미등록 404)
- `FavoriteControllerTest` 취소 3건 추가 (200 / 404 / 401)
- 전체 테스트 그린, `@SpringBootTest` 컨텍스트 정상 기동(H2)
- Postman 수동 테스트 완료 (등록 → 취소 200 → 재취소 404)
- 
<img width="499" height="477" alt="image" src="https://github.com/user-attachments/assets/33193f2e-3df6-4db6-b720-5850c9720468" />
<img width="506" height="493" alt="image" src="https://github.com/user-attachments/assets/0558a3b3-2e69-4325-8858-d73cccd53e7f" />
<img width="467" height="499" alt="image" src="https://github.com/user-attachments/assets/c54ae5fe-3f31-403a-a493-aa5df135bd1c" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/controller/FavoriteController.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+8/-0)
- `backend/src/test/java/com/dongnemarket/favorite/controller/FavoriteControllerTest.java` (+31/-0)
- `backend/src/test/java/com/dongnemarket/favorite/service/FavoriteServiceTest.java` (+27/-0)
- `docs/postman/favorite-remove.md` (+72/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 본인 관심만 (member_id 기준) 조회·삭제하므로 별도 권한 검증 불필요.
- 상품 존재/숨김/삭제 상태와 무관한 동작이라 상품 visibility 검증 없음 (이슈 #34와 독립).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/39
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/37
