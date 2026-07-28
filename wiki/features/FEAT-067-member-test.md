---
id: FEAT-067
type: feature
status: done
author: horangnabi97
date: 2026-06-29
related: []
tags: [member, backend, test]
pr: 67
---

## 무엇을 / 왜

Member 관련 테스트 보강

## 어떻게 (구현 요약)

- 담당 도메인: member
- 변경 파일:
  - `backend/src/test/java/com/dongnemarket/member/entity/MemberTest.java` (신규)
  - `backend/src/test/java/com/dongnemarket/member/controller/MemberControllerTest.java` (수정)

**MemberTest (신규)**
- `createAdmin_success`: ROLE_ADMIN, ACTIVE 상태로 생성됨을 검증
- `changeStatus_toDeleted_setsDeletedAt`: DELETED 시 deletedAt 기록됨을 검증
- `changeStatus_toSuspended_clearsDeletedAt`: SUSPENDED 시 deletedAt null 초기화 검증
- `changeStatus_toActive_clearsDeletedAt`: ACTIVE 시 deletedAt null 초기화 검증

**MemberControllerTest (수정)**
- `getMyInfo_invalidToken_returns401`: 기댓값 `"UNAUTHORIZED"` → `"INVALID_TOKEN"` 수정
  - 이유: 잘못된 토큰 요청 시 JwtAuthenticationFilter가 `INVALID_TOKEN`을 설정하고 EntryPoint가 그대로 반환. 토큰 없음(`UNAUTHORIZED`)과 구분되는 의도된 동작이었으나 기댓값이 잘못 작성되어 있었음

## 건드린 파일

- `backend/src/test/java/com/dongnemarket/member/controller/MemberControllerTest.java` (+2/-2)
- `backend/src/test/java/com/dongnemarket/member/entity/MemberTest.java` (+60/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/67
- 이슈: 없음
