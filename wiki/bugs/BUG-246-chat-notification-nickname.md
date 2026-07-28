---
id: BUG-246
type: bug
status: done
author: Crispy-down
date: 2026-07-27
related: []
tags: [알림, 채팅, 탈퇴, 드리프트, backend]
pr: 246
---

## 증상

채팅 알림 문구에 상대방 닉네임이 들어가 있었다. 그런데 채팅 알림은 저장형이 아니라 **안 읽은 채팅방을 조회하는 시점에 파생**되기 때문에, 상대가 나중에 탈퇴하면 과거 알림까지 소급해서 `탈퇴한 사용자`로 표시되는 드리프트가 발생한다.

## 원인

문구 생성 시점이 아니라 **조회 시점에** `getOpponent().getNickname()`을 읽는 구조였다. 알림 내용이 고정된 값이 아니라 조회할 때마다 상대 회원의 현재 상태를 따라가므로, 회원 상태가 바뀌면 과거 알림의 의미도 함께 바뀐다.

## 해결 방법

문구에서 닉네임을 아예 빼고 상품명만 남겼다 — `🔔 "{상품명}"에 새로운 채팅이 도착했습니다!`

- `NotificationService.buildChatMessage`에서 닉네임 인자 제거
- `toChatNotification`에서 `getOpponent().getNickname()` 조회 제거

저장형으로 전환하는 방법도 있었지만, 그러면 방 읽음 처리(`POST /read`)와 알림 상태가 어긋난다. 문구에서 닉네임을 빼는 쪽이 마스킹·드리프트 논쟁을 원천 제거하면서 비용이 가장 낮았다.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/notification/service/NotificationService.java`
- `backend/src/test/java/com/dongnemarket/notification/controller/NotificationControllerTest.java`

## 재발 방지

- `NotificationControllerTest`의 단언을 교체 — 문구가 **상품명은 포함하되 상대 닉네임은 포함하지 않을 것**을 검증. 방 구분은 닉네임이 아니라 `roomId`로 한다.
- 파생·병합·읽음 소멸 테스트 회귀 없음, notification 스위트 전체 그린.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/246
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/245
