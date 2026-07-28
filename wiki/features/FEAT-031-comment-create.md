---
id: FEAT-031
type: feature
status: done
author: Crispy-down
date: 2026-06-26
related: []
tags: [comment, backend, docs]
pr: 31
---

## 무엇을 / 왜

[Comment] 댓글 작성 API 구현

## 어떻게 (구현 요약)

- 댓글 작성 API (`POST /api/products/{productId}/comments`)
- 로그인 사용자 식별(`@AuthenticationPrincipal`), 상품 존재 검증
- `ApiResponse<CommentResponse>` 반환, Swagger 문서화
- `CommentCreateRequest`에 public 생성자 추가 — PR-1 골격엔 protected 생성자만 있어 Jackson이 요청 본문을 역직렬화하지 못하던 문제 수정 (검증된 `SignupRequest` 패턴과 동일)

**검증**

- `CommentServiceTest` 2건 (성공 / 상품없음)
- `CommentControllerTest` 4건 (201 / 401 / 404 / 400, 실제 JWT로 `@AuthenticationPrincipal` 바인딩 검증)
- 전체 테스트 그린, `@SpringBootTest` 컨텍스트 정상 기동(H2)
- Postman 수동 테스트 완료 (로그인 → 상품 등록 → 댓글 작성 201)
<img width="482" height="472" alt="image" src="https://github.com/user-attachments/assets/b776a7bb-9ef1-4fe5-b0ff-3e02c3045503" />
<img width="525" height="450" alt="image" src="https://github.com/user-attachments/assets/cc711984-faa9-4486-b0c8-6914abd865c1" />
<img width="515" height="462" alt="image" src="https://github.com/user-attachments/assets/2d5462fd-c3ff-43ba-b4fe-e281cf4896ca" />
<img width="471" height="450" alt="image" src="https://github.com/user-attachments/assets/5820252c-dba5-4a0a-a27d-c5395d2a85a1" />
<img width="528" height="442" alt="image" src="https://github.com/user-attachments/assets/09a1a584-d00a-4d59-a591-4670b44aaa4a" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/controller/CommentController.java` (+38/-0)
- `backend/src/main/java/com/dongnemarket/comment/dto/CommentCreateRequest.java` (+4/-0)
- `backend/src/main/java/com/dongnemarket/comment/service/CommentService.java` (+35/-0)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+126/-0)
- `backend/src/test/java/com/dongnemarket/comment/service/CommentServiceTest.java` (+65/-0)
- `docs/postman/comment-create.md` (+107/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 상품 존재 검증은 `productRepository.existsById` 사용. 현재 소프트삭제(`deleted_at`)·숨김(`hidden`) 상품도 존재로 간주됨. 차단이 필요하면 Product 도메인에 필터 조회 메서드 추가 협의 필요 (Favorite PR과 동일 사안).
- `CommentUpdateRequest`도 동일하게 public 생성자가 없어 역직렬화가 안 됨 → 댓글 수정(다음 PR) 작업 시 함께 수정 예정.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/31
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/24
