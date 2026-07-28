---
id: FEAT-141
type: feature
status: done
author: Crispy-down
date: 2026-07-06
related: [FEAT-089, FEAT-107, FEAT-149]
tags: [global, notification, backend]
pr: 141
---

## 무엇을 / 왜

알림 도메인 골격 및 댓글 생성 이벤트 (PR-N1a)

## 어떻게 (구현 요약)

사용자 알림 기능(저장+pull)의 첫 단위 — **내 상품 새 댓글 알림** 저장을 위한 `notification` 도메인 **골격**.
- `Notification` 엔티티: `recipient`(@ManyToOne LAZY) / `type` / `message`(렌더 스냅샷) / `productId`(연관 아닌 **id 스냅샷**, 상품 삭제·개명에도 알림 보존) / `isRead` / `lastNotifiedAt`. 프록시 안전 게터 `getRecipientId()`, 정적 팩토리 `of`.
- 상품별 **코얼레싱**용 도메인 메서드: `renotify()`(같은 상품 재이벤트 시 새 row 대신 시각만 갱신), `markRead()`.
- `NotificationType`(현재 `COMMENT`), `NotificationRepository`(코얼레싱 조회 + 목록 조회 시그니처).
- `CommentCreatedEvent(recipientId, productId, productTitle, commenterId)` record.

**검증**

- `./gradlew compileJava` **BUILD SUCCESSFUL**.
- **행위 테스트는 PR-N1b(발행·핸들러·조회 API)에서 커버**한다. 이 PR은 실행 로직이 없는 골격이라 별도 행위 테스트를 두지 않는다(채팅 PR1a #107 골격과 동일 방침).

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/common/event/CommentCreatedEvent.java` (+15/-0)
- `backend/src/main/java/com/dongnemarket/notification/entity/Notification.java` (+97/-0)
- `backend/src/main/java/com/dongnemarket/notification/entity/NotificationType.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/notification/repository/NotificationRepository.java` (+22/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **팀장 영역 파일 추가**: `global/common/event/CommentCreatedEvent.java`. favoriteCount 이벤트(#89) 선례대로 이벤트 record를 이 위치에 두었습니다 — 위치 승인 부탁드립니다.
- **신규 도메인 패키지** `notification` 생성(권건우 후속 담당). ErrorCode는 손대지 않았습니다(골격 단계 신규 없음, NOTIFICATION 섹션 필요 시 N1b에서 팀장 협의).
- **코얼레싱 레이스**(동시 댓글 시 안읽음 중복 가능)는 호출부가 없는 골격엔 무영향이며, 파인더 시그니처 결정과 함께 **PR-N1b 착수 시 처리**합니다(이슈 #138에 결정 포인트 기록).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/141
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/137
