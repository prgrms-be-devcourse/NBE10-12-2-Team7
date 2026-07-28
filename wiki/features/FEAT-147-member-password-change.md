---
id: FEAT-147
type: feature
status: done
author: horangnabi97
date: 2026-07-06
related: [FEAT-146]
tags: [global, member, backend, docs]
pr: 147
---

## 무엇을 / 왜

로그인 사용자 비밀번호 변경 기능 추가

## 어떻게 (구현 요약)

- 로그인한 사용자의 비밀번호 변경 기능 추가 (PATCH /api/members/me/password)
- 현재 비밀번호 확인, 새 비밀번호는 회원가입과 동일한 정책 적용
- 변경 성공 시 기존 Refresh Token 삭제(보안 강화 — 다른 세션도 재로그인 필요)

**검증**

- `./gradlew test` 420개 통과 (0 failed, 0 skipped) — develop 최신(이메일 인증 PR #146 포함) 기준으로 rebase 후 재검증
- 실제 MySQL 대상으로 curl 직접 호출해 전 케이스 검증 완료 (`docs/postman/member-password-change.md`)
  - 성공/재로그인 확인/기존 Refresh Token 무효화/현재비밀번호불일치/새비밀번호동일/정책위반/미인증
- DB에서 비밀번호가 BCrypt 해시로 저장됨을 직접 확인

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/member/controller/MemberController.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/member/dto/PasswordChangeRequest.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/Member.java` (+5/-0)
- `backend/src/main/java/com/dongnemarket/member/service/MemberService.java` (+29/-1)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberControllerTest.java` (+133/-0)
- `backend/src/test/java/com/dongnemarket/member/service/MemberServiceTest.java` (+98/-0)
- `docs/postman/member-password-change.md` (+138/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/147
- 이슈: 없음
