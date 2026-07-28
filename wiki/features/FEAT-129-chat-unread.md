---
id: FEAT-129
type: feature
status: done
author: Crispy-down
date: 2026-07-05
related: []
tags: [chat, backend]
pr: 129
---

## 무엇을 / 왜

채팅방 안읽음 메시지 수 및 읽음 처리 (PR1c)

## 어떻게 (구현 요약)

- **안읽음 카운트**: `GET /api/chat-rooms` 응답의 각 방에 `unreadCount` 추가.
  - 안읽음 = 그 방에서 `상대가 보낸(sender ≠ 나)` + `내 읽음 지점 이후(id > last_read)` 메시지 수. 내 메시지는 제외.
  - 좌석(구매자/판매자)별 읽음 지점을 `CASE`로 분기 + `COALESCE(.., 0)`(null=전부 안읽음) + `GROUP BY` **단일 쿼리**로 목록 N개를 한 번에 집계(N+1 회피, 기존 마지막메시지 조회와 동일 패턴). `getMyRooms` 2→3쿼리.
- **읽음 처리**: `POST /api/chat-rooms/{roomId}/read` 신설. 내 읽음 지점을 방 최신 메시지 id까지 전진(참여자만, 빈 방은 no-op). 읽음 지점은 단조 전진만.
- **저장 방식**: `ChatRoom`에 `buyer_last_read_message_id` / `seller_last_read_message_id` 컬럼 2개 + 도메인 메서드(`lastReadMessageIdOf`/`markRead`). 1:1이라 참여자 2명 고정 → 별도 읽음 테이블 대신 방 컬럼으로 카운트 쿼리 단순화.

**검증**

- ChatControllerTest 통합 테스트 추가(전체 스위트 `./gradlew test` BUILD SUCCESSFUL):
  - GetMyRooms: 같은 사용자가 A방=구매자·B방=판매자일 때 좌석별 카운트 정확성 + 내가 보낸 메시지 제외 검증(→ CASE 양쪽 분기 모두 실행)
  - MarkAsRead: 읽음 처리 후 해당 방만 0 / 비참여자 403 CHAT_ACCESS_DENIED / 없는 방 404 CHAT_ROOM_NOT_FOUND
- Postman 수동 테스트: buyer/seller 로그인 → seller 메시지 2건 → buyer 목록 `unreadCount=2` → `POST /read` → 목록 `unreadCount=0` 시나리오 확인.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/controller/ChatController.java` (+9/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatRoomListResponse.java` (+12/-5)
- `backend/src/main/java/com/dongnemarket/chat/entity/ChatRoom.java` (+34/-0)
- `backend/src/main/java/com/dongnemarket/chat/repository/ChatMessageRepository.java` (+21/-0)
- `backend/src/main/java/com/dongnemarket/chat/repository/RoomUnreadCount.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/chat/service/ChatService.java` (+22/-1)
- `backend/src/test/java/com/dongnemarket/chat/controller/ChatControllerTest.java` (+82/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **스키마**: 마이그레이션 도구 미사용, `ddl-auto`(dev/local `update`)라 앱 기동 시 `chat_rooms`에 컬럼 2개가 자동 추가됩니다. 별도 DDL/마이그레이션 스크립트 없음.
- **신규 ErrorCode 없음**: 읽음 엔드포인트는 기존 `CHAT_ROOM_NOT_FOUND`/`CHAT_ACCESS_DENIED` 재사용.
- **`?after={lastId}` 순방향 폴링은 이 PR에서 분리(별도 후속 PR)**: 안읽음/읽음과 독립 단위이고(폴링은 기존 최신-재조회+중복제거로 이미 동작), 현재 커서(과거 방향 DESC)와 모드가 충돌해 분리했습니다.
- **프론트 연동은 유진님 담당**: 목록 배지 렌더 + 방 입장 시 `POST /read` 호출. 핸드오프 문서 `docs/design/chat-frontend-unread-handoff.md`(로컬 공유, 미커밋).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/129
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/127
