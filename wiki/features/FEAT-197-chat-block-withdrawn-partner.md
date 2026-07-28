---
id: FEAT-197
type: feature
status: done
author: Crispy-down
date: 2026-07-08
related: []
tags: [chat, global, member, backend]
pr: 197
---

## 무엇을 / 왜

탈퇴한 상대에게 메시지 전송 차단 (읽기는 유지)

## 어떻게 (구현 요약)

탈퇴(DELETED)한 상대와의 채팅방에서 **메시지 전송(쓰기)만 차단**하고, 히스토리 조회·방 목록(읽기)은 유지합니다.

- `ChatService.sendMessage`에 참여자 검증 **직후** 상대 탈퇴 가드 추가 → 상대가 탈퇴 상태면 `CHAT_PARTNER_WITHDRAWN`(400)
  - 참여자 검증을 먼저 수행해 비참여자에게 상대 상태를 노출하지 않음
  - 양방향 적용: 판매자 탈퇴 → 구매자 차단 / 구매자 탈퇴 → 판매자 차단 (`opponentOf` 대칭)
- `Member.isWithdrawn()` 신설 — "탈퇴" 판정의 단일 기준점(SSOT). `getDisplayNickname()`이 이를 재사용해 마스킹/전송차단이 동일 정의를 공유
- `ErrorCode.CHAT_PARTNER_WITHDRAWN`(400, CHAT_004) 추가

**검증**

- `ChatControllerTest`에 3개 추가:
  - 판매자 탈퇴 → 구매자 전송 400 `CHAT_PARTNER_WITHDRAWN`
  - 구매자 탈퇴 → 판매자 전송 400 (양방향)
  - 상대 탈퇴여도 메시지 조회·방 목록 200 유지(읽기 보존)
- 기존 `send_201`(정상 전송) 그대로 통과 → 과잉 차단 없음 확인
- `./gradlew test` 전체 스위트 **BUILD SUCCESSFUL**

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/service/ChatService.java` (+8/-1)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/Member.java` (+11/-3)
- `backend/src/test/java/com/dongnemarket/chat/controller/ChatControllerTest.java` (+48/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **범위 밖(관련 갭, 상품 가시성 = 한상민 소관)**: `createRoom`이 쓰는 `validateAccessibleProduct`가 판매자 status를 검사하지 않아 탈퇴 판매자 상품에 방 **레코드 생성**은 여전히 가능합니다. 다만 `sendMessage`가 전송 시점에 상대 status를 읽으므로 그렇게 만든 방의 첫 전송도 차단됩니다 → **쓰기가 새는 hole은 아니며**, 방 레코드 생성 자체를 막는 것은 별도 논의 대상입니다.
- SUSPENDED(관리자 정지)는 탈퇴가 아니므로 차단하지 않음(마스킹 정책과 일관).
- 탈퇴한 사용자가 유효 토큰으로 요청하는 케이스는 인증/토큰 수명(김대연) 소관으로 범위 밖.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/197
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/194
