---
id: FEAT-191
type: feature
status: done
author: Crispy-down
date: 2026-07-07
related: []
tags: [comment, backend]
pr: 191
---

## 무엇을 / 왜

댓글 목록에 작성자 닉네임 노출 및 탈퇴 사용자 마스킹

## 어떻게 (구현 요약)

댓글 목록(`GET /api/products/{productId}/comments`) 응답에 **작성자 닉네임(`authorNickname`)** 을 추가했습니다. 탈퇴(`MemberStatus.DELETED`) 작성자는 `"탈퇴한 사용자"`로 마스킹되며, **댓글 내용은 그대로 보존**됩니다.

- **`CommentResponse`에 `authorNickname` 추가**: `comment.getMember().getDisplayNickname()`으로 채워 탈퇴 작성자를 자동 마스킹 (`Member.getDisplayNickname()` SSOT 재사용, #178). email 등 민감정보 없이 닉네임만 노출.
- **목록 쿼리 N+1 제거**: 파생 쿼리 → `@Query ... JOIN FETCH c.member`로 전환. 단건 `@ManyToOne`이라 행 증식/HHH000104 없음.
- **내용 보존은 코드 변경 없음**: 회원 `softDelete()`는 댓글에 cascade하지 않으므로 작성자 탈퇴와 무관하게 내용 유지.

**검증**

- `CommentControllerTest`에 검증 추가:
  - 목록에 작성자 닉네임 노출(`writer`/`seller`)
  - **탈퇴 작성자 → `authorNickname: "탈퇴한 사용자"`, `content`는 원문 유지**
- `CommentServiceTest`의 "접근 불가 시 조회 미실행" 검증을 새 메서드명(`findAllWithMemberByProduct_Id`)으로 갱신
- `comment.*` 전체 **BUILD SUCCESSFUL**

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/dto/CommentResponse.java` (+6/-1)
- `backend/src/main/java/com/dongnemarket/comment/repository/CommentRepository.java` (+11/-1)
- `backend/src/main/java/com/dongnemarket/comment/service/CommentService.java` (+1/-1)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+17/-1)
- `backend/src/test/java/com/dongnemarket/comment/service/CommentServiceTest.java` (+1/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **정책 비대칭 승인**: 탈퇴 시 상품은 숨김(404, 판매자 status 필터)인 반면 댓글은 **보존 + 이름 마스킹**. 대화 맥락 유지를 위한 의도된 구분(대부분 플랫폼의 "삭제된 사용자" 표기와 동일).
- **API 계약 변경**: 응답에 `authorNickname` 필드 추가 → **프론트 연동 필요**.
- 마스킹 문구 `"탈퇴한 사용자"` 확정, `DELETED`만 마스킹(`SUSPENDED` 제외) 정책 동의.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/191
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/190
