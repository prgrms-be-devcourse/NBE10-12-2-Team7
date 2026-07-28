---
id: FEAT-064
type: feature
status: done
author: horangnabi97
date: 2026-06-29
related: []
tags: [member, backend]
pr: 64
---

## 무엇을 / 왜

Admin용 Member 엔티티 메서드 2개 추가

## 어떻게 (구현 요약)

- 담당 도메인: member
- 변경 파일: `backend/src/main/java/com/dongnemarket/member/entity/Member.java`

Admin 팀원 요청으로 Member 엔티티에 메서드 2개 추가:

1. `createAdmin(email, password, nickname)` — ROLE_ADMIN/ACTIVE 관리자 시드 계정 생성용 팩토리 메서드 (비밀번호는 호출부에서 BCrypt 인코딩해 전달)
2. `changeStatus(MemberStatus)` — 관리자 회원 상태 변경; DELETED 시 deletedAt 기록, 그 외(ACTIVE/SUSPENDED) null 초기화

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/member/entity/Member.java` (+11/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/64
- 이슈: 없음
