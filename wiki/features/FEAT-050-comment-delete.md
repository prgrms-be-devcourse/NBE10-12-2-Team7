---
id: FEAT-050
type: feature
status: done
author: Crispy-down
date: 2026-06-26
related: []
tags: [comment, backend, docs]
pr: 50
---

## 무엇을 / 왜

[Comment] 댓글 삭제 API 구현

## 어떻게 (구현 요약)

- 댓글 삭제 API (`DELETE /api/comments/{commentId}`)
- `commentId`로 댓글 조회(소프트삭제 제외) → 작성자 본인 검증 → 소프트 삭제(`deleted_at` 설정)
- `ApiResponse<Void>` 반환(200, data 없음), Swagger 문서화

**검증**

- `CommentServiceTest` 삭제 3건 추가 (성공 / 404 / 403)
- `CommentControllerTest` 삭제 4건 추가 (200 / 403 / 404 / 401)
- comment 도메인 테스트 전체 그린
- Postman 수동 테스트 완료 (작성 → 삭제 200 → 재삭제 404로 소프트삭제 확인)
<img width="468" height="682" alt="image" src="https://github.com/user-attachments/assets/b9333def-05be-4fd1-8de9-1da4517139bd" />
<img width="447" height="678" alt="image" src="https://github.com/user-attachments/assets/81daf385-3929-4961-bf73-6812739430fa" />
<img width="525" height="685" alt="image" src="https://github.com/user-attachments/assets/a2307a88-7ba8-48af-8f10-31ba5e9ea84f" />
<img width="373" height="664" alt="image" src="https://github.com/user-attachments/assets/537a3f84-78c6-48ae-a71c-6d11e08fabcd" />
<img width="484" height="639" alt="image" src="https://github.com/user-attachments/assets/45192827-71a0-4a88-983b-51fd9635e6e9" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/controller/CommentController.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/comment/service/CommentService.java` (+11/-0)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+40/-0)
- `backend/src/test/java/com/dongnemarket/comment/service/CommentServiceTest.java` (+32/-0)
- `docs/postman/comment-delete.md` (+89/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 작성자 본인만 삭제 (`COMMENT_OWNER_ONLY`). 관리자 삭제 권한은 Admin이 댓글 기능 붙일 때 별도 협의.
- 삭제는 `commentId` 기준 + 작성자 검증이라 상품 도메인과 무관.

> ⚠️ 참고: 현재 develop에서 `MemberControllerTest`(유효하지 않은 토큰 → 401) 1건이 실패하는데, 이는 글로벌 보안 변경(`cc5e33c`)으로 invalid token 응답이 `UNAUTHORIZED`→`INVALID_TOKEN`으로 바뀐 데 따른 **기존 실패로 본 PR과 무관**합니다. (stash 후 순정 develop에서도 동일 실패 확인) member/global 도메인 영역이라 이미 팀에 공유됨.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/50
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/49
