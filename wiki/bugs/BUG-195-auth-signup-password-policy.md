---
id: BUG-195
type: bug
status: done
author: horangnabi97
date: 2026-07-08
related: []
tags: [auth, member, signup, backend, frontend]
pr: 195
---

## 증상

회원가입 비밀번호 정책을 변경/재설정 정책과 통일 — 상세 증상 미기록.

## 원인

- PR 본문에 원인 기록 없음.

## 해결 방법

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `auth` 도메인 — SignupRequest(DTO, 수정)
- 라우트 `/signup` — page.tsx(수정)
- 테스트 — 3개 파일 (+48줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/dto/SignupRequest.java` (+6/-1)
- `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java` (+26/-26)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberControllerTest.java` (+13/-13)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberLocationControllerTest.java` (+9/-9)
- `frontend/src/app/signup/page.tsx` (+4/-4)

## 재발 방지

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/195
- 이슈: 없음
