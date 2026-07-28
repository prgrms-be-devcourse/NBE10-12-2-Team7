---
id: FEAT-158
type: feature
status: done
author: Crispy-down
date: 2026-07-06
related: []
tags: [notification, backend]
pr: 158
---

## 무엇을 / 왜

알림 피드에 채팅 파생 병합 + 안읽음 카운트 (PR-N2)

## 어떻게 (구현 요약)

내 알림 피드에 **안읽은 채팅방을 CHAT 알림으로 파생 병합**한다(저장 안 함). PR-N1b(#138) 위에 채팅분과 배지를 얹었다.
- **`GET /api/notifications` 병합**: 저장형 댓글 알림 ⊕ `ChatService.getMyRooms`의 `unreadCount>0` 방을 CHAT으로 변환 → `occurredAt` DESC 정렬. 채팅은 **방마다 1건**이라 같은 상품에 구매자가 여럿이면 **구매자별 별도 알림**이 된다(닉네임+roomId로 구분, 코얼레싱 안 함). 방 읽음 처리 시 자동 소멸.
  - CHAT 문구: `"{상품명}"에 대해 "{상대닉네임}"님의 새로운 채팅이 도착했습니다!`
- **`GET /api/notifications/unread-count`(신규)**: 안읽은 댓글 알림 수 + 안읽은 채팅방 수 합산(헤더 배지).
- **응답**: dto 전용 `NotificationFeedType{COMMENT,CHAT}` + `roomId`(CHAT만, null이면 생략). 저장용 `NotificationType`엔 CHAT 미추가(파생은 저장 안 하므로 엔티티 enum은 clean 유지).

**검증**

- **`./gradlew test` 전체 그린**(BUILD SUCCESSFUL).
- `NotificationControllerTest` 추가: 안읽은 방→CHAT(roomId) / **구매자 여럿→방별 개별 CHAT 알림**(distinct roomId·닉네임) / **방 읽음 후 CHAT 소멸** / 댓글+채팅 병합 / unread-count 합산(=2) / 401.
- Postman 수동 테스트 예정 — 사용자 첨부
<img width="735" height="829" alt="image" src="https://github.com/user-attachments/assets/1690a743-4e55-4749-8d99-8525a10b1024" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/notification/controller/NotificationController.java` (+9/-0)
- `backend/src/main/java/com/dongnemarket/notification/dto/NotificationFeedType.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/notification/dto/NotificationResponse.java` (+23/-8)
- `backend/src/main/java/com/dongnemarket/notification/dto/NotificationUnreadCountResponse.java` (+17/-0)
- `backend/src/main/java/com/dongnemarket/notification/repository/NotificationRepository.java` (+3/-0)
- `backend/src/main/java/com/dongnemarket/notification/service/NotificationService.java` (+47/-5)
- `backend/src/test/java/com/dongnemarket/notification/controller/NotificationControllerTest.java` (+121/-3)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **크로스도메인 커플링 신규**: `NotificationService → ChatService`(파생 병합). 둘 다 내 도메인(notification+chat), ChatService는 역참조 없어 **순환 없음(DAG)**.
- **`POST /api/notifications/read`는 채팅분 배지를 지우지 않음(의도)**: 전체 읽음은 저장형 댓글 알림만 처리하고, **채팅 알림은 방 읽음(`POST /chat-rooms/{id}/read`)으로 소멸**한다. FE에서 "전체 읽음" 후에도 `unread-count>0`일 수 있음(채팅 미읽음 시) — 버그 아님.
- **`unread-count`는 호출마다 `getMyRooms`(3쿼리) 수행**: 개인 목록이라 바운드되어 MVP 수용. 배지 폴링 빈도 높으면 후속 최적화 여지(known cost).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/158
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/139
