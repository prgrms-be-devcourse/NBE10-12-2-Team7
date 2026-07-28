---
id: BUG-183
type: bug
status: done
author: Crispy-down
date: 2026-07-07
related: []
tags: [chat, member, backend]
pr: 183
---

## 증상

탈퇴한 사용자 닉네임을 채팅·알림에서 마스킹 — 상세 증상 미기록.

## 원인

- PR 본문에 원인 기록 없음.

## 해결 방법

탈퇴(`MemberStatus.DELETED`)한 회원의 **실명 닉네임**이 채팅 상대·판매자 표시와 채팅 알림 문구에 그대로 노출되던 문제를 수정했습니다.

- **`Member.getDisplayNickname()` 신설 (단일 진실 공급원)**: 탈퇴 회원이면 `"탈퇴한 사용자"`, 아니면 실명 반환. `SUSPENDED`는 관리자 정지일 뿐 탈퇴가 아니므로 마스킹하지 않습니다.
- **`ChatMemberSummary.of`가 `getDisplayNickname()`을 사용**하도록 변경.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/dto/ChatMemberSummary.java` (+1/-1)
- `backend/src/main/java/com/dongnemarket/member/entity/Member.java` (+12/-0)
- `backend/src/test/java/com/dongnemarket/chat/controller/ChatControllerTest.java` (+13/-0)

## 재발 방지

- `ChatControllerTest`에 "탈퇴한 상대는 닉네임이 '탈퇴한 사용자'로 마스킹된다" 통합 테스트 추가
- `ChatControllerTest` / `notification.*` / `member.*` **BUILD SUCCESSFUL**
- 활성 회원은 `getDisplayNickname == getNickname`이라 기존 동작 회귀 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/183
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/178
