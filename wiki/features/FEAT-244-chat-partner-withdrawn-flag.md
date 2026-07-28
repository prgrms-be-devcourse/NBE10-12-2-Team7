---
id: FEAT-244
type: feature
status: done
author: Crispy-down
date: 2026-07-27
related: []
tags: [chat, backend]
pr: 244
---

## 무엇을 / 왜

채팅 상대 탈퇴 여부(withdrawn) 응답 노출

## 어떻게 (구현 요약)

- `ChatMemberSummary`에 `withdrawn` boolean 필드 추가 (`Member.isWithdrawn()` 재사용 — 탈퇴 판정 SSOT)
- `ChatRoomListResponse.opponent`(목록)·`ChatRoomDetailResponse.seller`(입장) 모두 `ChatMemberSummary` 경유라 자동 노출
- 프론트가 탈퇴 상대 채팅에서 입력창을 프로액티브하게 비활성화하기 위한 신뢰 신호. 기존 닉네임 문자열(`탈퇴한 사용자`) 비교(취약)를 대체

**검증**

- `ChatControllerTest`: 목록 응답에서 탈퇴 상대 `opponent.withdrawn=true` + 마스킹 유지, 방 연결 응답에서 활성 `seller.withdrawn=false` 단언 추가
- chat·notification 스위트 전체 그린

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/dto/ChatMemberSummary.java` (+6/-2)
- `backend/src/test/java/com/dongnemarket/chat/controller/ChatControllerTest.java` (+6/-3)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **BE only PR입니다.** FE(탈퇴 상대면 입력창 disable + 안내 배너)는 별도 작업(유진님) — 핸드오프 문서 제공 예정
- 응답에 필드 1개 추가(비파괴적, 기존 `nickname` 마스킹 동작 무변경)
- 신규 ErrorCode 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/244
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/243
