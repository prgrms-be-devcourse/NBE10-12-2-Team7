---
id: FEAT-053
type: feature
status: done
author: Crispy-down
date: 2026-06-27
related: []
tags: [comment, backend, docs]
pr: 53
---

## 무엇을 / 왜

댓글 목록 조회 API 구현

## 어떻게 (구현 요약)

- 댓글 목록 조회 API (`GET /api/products/{productId}/comments`)
- 상품 존재 검증(`existsById`) → 삭제되지 않은 댓글만 조회 → `List<CommentResponse>` 반환
- 비로그인 사용자도 조회 가능 (SecurityConfig permitAll), 작성순(`created_at ASC`) 정렬
- 존재하지 않는 상품 조회 시 404 `PRODUCT_NOT_FOUND` (댓글 작성 API와 일관)
- `ApiResponse<List<CommentResponse>>`(200) 반환, Swagger 문서화

**검증**

- `CommentServiceTest` 목록 3건 추가 (성공 / 빈 목록 / 404)
- `CommentControllerTest` 목록 3건 추가 (비로그인 200·작성순·삭제제외 / 빈배열 / 404)
- comment 도메인 테스트 전체 그린 (Service 11 · Controller 15 · Repository 3)
- Postman 수동 테스트 완료 (목록 작성순 조회 / 삭제된 댓글 제외 / 빈 배열 / 없는 상품 404)
<img width="497" height="823" alt="image" src="https://github.com/user-attachments/assets/b7549bd6-4462-4293-a30d-f730d2a96f5a" />
<img width="883" height="715" alt="image" src="https://github.com/user-attachments/assets/ddbe60df-aa76-4497-a7ff-b10b975b7ac9" />
<img width="447" height="426" alt="image" src="https://github.com/user-attachments/assets/6bd8c162-a51e-42cd-8dd2-01cf24e64dbd" />
<img width="469" height="481" alt="image" src="https://github.com/user-attachments/assets/dc7b8d15-20fb-4004-ac2f-1c837f11fa3b" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/controller/CommentController.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/comment/repository/CommentRepository.java` (+1/-1)
- `backend/src/main/java/com/dongnemarket/comment/service/CommentService.java` (+12/-0)
- `backend/src/test/java/com/dongnemarket/comment/CommentRepositoryTest.java` (+2/-2)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+35/-0)
- `backend/src/test/java/com/dongnemarket/comment/service/CommentServiceTest.java` (+41/-0)
- `docs/postman/comment-list.md` (+89/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- MVP 범위로 기존 `CommentResponse` DTO 그대로 사용 (작성자 닉네임 등 응답 확장은 팀 회의 후 별도 PR).
- 정렬 기준은 작성순(`created_at ASC`)으로 고정. 최신순 등 정책 변경 필요하면 알려주세요.
- 조회 API라 products 테이블은 존재 검증만 하며 수정하지 않음 (담당 도메인 외 변경 없음).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/53
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/52
