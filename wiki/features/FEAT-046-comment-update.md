---
id: FEAT-046
type: feature
status: done
author: Crispy-down
date: 2026-06-26
related: []
tags: [comment, backend, docs]
pr: 46
---

## 무엇을 / 왜

[Comment] 댓글 수정 API 구현

## 어떻게 (구현 요약)

- 댓글 수정 API (`PATCH /api/comments/{commentId}`)
- `commentId`로 댓글 조회(소프트삭제 제외) → 작성자 본인 검증 → 내용 변경
- `CommentUpdateRequest`에 public 생성자 추가 — protected 생성자만 있어 Jackson 역직렬화가 안 되던 문제 수정 (댓글 작성 PR과 동일 사안)
- `ApiResponse<CommentResponse>` 반환, Swagger 문서화

**검증**

- `CommentServiceTest` 수정 3건 추가 (성공 / 404 / 403)
- `CommentControllerTest` 수정 4건 추가 (200 / 403 / 404 / 401)
- 전체 테스트 그린, `@SpringBootTest` 컨텍스트 정상 기동(H2)
- Postman 수동 테스트 완료 (작성 → 수정 200, 작성자 아님 403)
- 
<img width="438" height="664" alt="image" src="https://github.com/user-attachments/assets/513b7782-1a9e-48a9-8f81-72682ecc25dc" />
<img width="454" height="627" alt="image" src="https://github.com/user-attachments/assets/f118031b-ac89-4141-9dd7-a154682abf76" />
<img width="498" height="628" alt="image" src="https://github.com/user-attachments/assets/226ab55e-870c-431d-87a6-1031e28a7a0b" />
<img width="404" height="603" alt="image" src="https://github.com/user-attachments/assets/95f347ce-c575-4943-b12f-206895a3ba7c" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/controller/CommentController.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/comment/dto/CommentUpdateRequest.java` (+4/-0)
- `backend/src/main/java/com/dongnemarket/comment/service/CommentService.java` (+13/-0)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+54/-0)
- `backend/src/test/java/com/dongnemarket/comment/service/CommentServiceTest.java` (+36/-0)
- `docs/postman/comment-update.md` (+125/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 수정은 `commentId` 기준 조회 + 작성자(`member_id`) 검증이라 상품 도메인과 무관 (이슈 #34와 독립).
- 이미 소프트삭제(`deleted_at`)된 댓글은 조회에서 제외되어 404 처리.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/46
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/45
