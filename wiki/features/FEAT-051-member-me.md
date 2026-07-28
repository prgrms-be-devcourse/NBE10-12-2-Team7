---
id: FEAT-051
type: feature
status: done
author: horangnabi97
date: 2026-06-27
related: []
tags: [member, backend, docs]
pr: 51
---

## 무엇을 / 왜

내 정보 조회/수정/탈퇴 API 상태 체크 및 Postman 문서 추가

## 어떻게 (구현 요약)

JWT가 유효하더라도 DELETED/SUSPENDED 상태의 회원이 `/api/members/me` 엔드포인트에 접근 가능한 보안 허점을 수정했습니다.

`MemberService`에 `validateActiveMember()` 메서드를 추가해 GET/PATCH/DELETE 세 메서드 모두 상태 체크를 적용했습니다.

**검증**

| 테스트 | 케이스 수 | 결과 |
|---|---|---|
| MemberServiceTest | 13개 | 0 failures, 0 errors |
| MemberControllerTest | 16개 | 0 failures, 0 errors |

**신규 케이스 (각 메서드 × DELETED/SUSPENDED)**
- GET: 탈퇴 회원 → 400, 정지 회원 → 403
- PATCH: 탈퇴 회원 → 400, 정지 회원 → 403
- DELETE: 재탈퇴 시도 → 400, 정지 회원 → 403

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/member/service/MemberService.java` (+13/-0)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberControllerTest.java` (+85/-0)
- `backend/src/test/java/com/dongnemarket/member/service/MemberServiceTest.java` (+75/-0)
- `docs/postman/member-me.md` (+373/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/51
- 이슈: 없음
