---
id: FEAT-128
type: feature
status: done
author: Crispy-down
date: 2026-07-04
related: []
tags: [chat, backend]
pr: 128
---

## 무엇을 / 왜

[Feature] 채팅방 목록을 마지막 메시지 시각순으로 정렬

## 어떻게 (구현 요약)

- 내 채팅방 목록(`GET /api/chat-rooms`)의 정렬을 **방 생성순(id DESC)** → **최근 활동순(마지막 메시지 시각 DESC)** 으로 변경.
- `ChatService.getMyRooms`에서 **인메모리 정렬**: 이미 로딩한 방별 마지막 메시지(`findLatestPerRoom`)로 정렬한다. 이 API는 페이지네이션 없이 전체 목록을 반환하고 개인 목록이라 방 수가 작아 **DB 비정규화 없이** 서비스 계층에서 처리 가능(관심목록과 동일 판단).
- 정렬 규칙: 활동 시각 = `마지막 메시지 createdAt`, 메시지 없는 방은 `방 createdAt`(갓 만든 빈 방이 상단). 동시각 타이브레이크 = `roomId DESC`.
- 리포지토리/서비스 Javadoc의 "PR3(비정규화) 범위" 문구를 "서비스단 재정렬"로 갱신.

**검증**

- ControllerTest(통합) 추가: 최근 메시지 방이 먼저, 오래된 메시지 방이 뒤, 메시지 없는 방은 생성 시각 기준으로 정렬됨을 검증(`GetMyRooms` 그룹, 5개 그린).
- `./gradlew test --tests "com.dongnemarket.chat.*"` → **BUILD SUCCESSFUL** (실패·에러 0).
- 프론트(`frontend/src/app/chat/page.tsx`)는 목록을 받은 순서 그대로 `map` 렌더하므로 백엔드 정렬이 그대로 반영됨.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/repository/ChatRoomRepository.java` (+4/-2)
- `backend/src/main/java/com/dongnemarket/chat/service/ChatService.java` (+21/-1)
- `backend/src/test/java/com/dongnemarket/chat/controller/ChatControllerTest.java` (+27/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **DB 비정규화(last-message 컬럼) 없이 인메모리 정렬**을 택한 근거: 목록 API가 무페이지네이션 + 개인 목록(방 수 작음). 트래픽 급증 시 last-message 비정규화는 후속(PR3) 여지로 남김.
- 빈 방(메시지 없음)은 방 생성 시각을 활동 시각으로 보아 정렬 — 상단 노출 의도.
- 기존 `findMyChatRooms` 쿼리의 `ORDER BY id DESC`는 결정적 입력을 위해 유지(최종 정렬은 서비스에서 재적용).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/128
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/126
