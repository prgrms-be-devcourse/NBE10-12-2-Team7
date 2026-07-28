---
id: FEAT-149
type: feature
status: done
author: Crispy-down
date: 2026-07-06
related: [FEAT-141]
tags: [comment, notification, backend]
pr: 149
---

## 무엇을 / 왜

댓글 알림 발행·핸들러·조회 API (PR-N1b)

## 어떻게 (구현 요약)

내 상품에 **새 댓글이 달리면 소유자에게 댓글 알림**을 저장한다(저장+pull). PR-N1a(#141) 골격 위에 실제 동작을 얹었다.
- **발행**: `CommentService.create`에서 댓글 저장 후 `CommentCreatedEvent` 발행. 자기 상품 내 댓글은 스킵(`recipientId == memberId`). `product.getMember().getId()`는 프록시 id 접근이라 추가 쿼리 없음.
- **핸들러**: `NotificationEventHandler` — `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`. 댓글 커밋 후 별도 트랜잭션에서 저장(best-effort: 알림 실패가 댓글 작성을 롤백하지 않음).
- **코얼레싱(옵션 B)**: `NotificationService.notifyComment` — 같은 상품 안읽은 알림이 있으면 새 row 대신 `renotify()`. 동시 이벤트 레이스로 2행 이상이면 **최신 1개만 갱신 + 나머지 삭제 → 1행으로 수렴**(MySQL 부분 유니크 미지원을 애플리케이션이 수렴, 이슈 #138 결정).
- **API**: `GET /api/notifications`(최근순, `Limit(100)`), `POST /api/notifications/read`(벌크 읽음). `NotificationResponse{type,message,productId,isRead,occurredAt}`.

**검증**

- **`./gradlew test` 전체 그린**(BUILD SUCCESSFUL).
- `NotificationControllerTest`(통합): 남이 내 상품에 댓글→알림 1건 / 같은 상품 다중 댓글→**코얼레싱 1건** / 자기 댓글→미발행 / 전체 읽음 처리 / 401. (AFTER_COMMIT은 MockMvc 실제 커밋으로 발동)
- `NotificationServiceTest`(통합): **레이스 수렴**(안읽음 2행 사전삽입 → notifyComment → 1행) / 없을 때 신규 저장.
- Postman 수동 테스트 예정 — 사용자 첨부
<img width="606" height="723" alt="image" src="https://github.com/user-attachments/assets/998eebb3-b4d0-44ce-80bb-2471413c8775" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/service/CommentService.java` (+13/-1)
- `backend/src/main/java/com/dongnemarket/notification/controller/NotificationController.java` (+41/-0)
- `backend/src/main/java/com/dongnemarket/notification/dto/NotificationResponse.java` (+45/-0)
- `backend/src/main/java/com/dongnemarket/notification/event/NotificationEventHandler.java` (+34/-0)
- `backend/src/main/java/com/dongnemarket/notification/repository/NotificationRepository.java` (+16/-7)
- `backend/src/main/java/com/dongnemarket/notification/service/NotificationService.java` (+73/-0)
- `backend/src/test/java/com/dongnemarket/comment/controller/CommentControllerTest.java` (+6/-0)
- `backend/src/test/java/com/dongnemarket/notification/controller/NotificationControllerTest.java` (+189/-0)
- `backend/src/test/java/com/dongnemarket/notification/service/NotificationServiceTest.java` (+104/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **`CommentService` 변경 포함**: 알림 패키지뿐 아니라 **댓글 작성 흐름에 이벤트 발행**을 추가했습니다(내 도메인이라 발행 위치로 적절). 댓글 흐름 diff를 함께 봐주세요.
- **팀장 이벤트 위치 ack가 이제 실효**: N1a(#141)에서 추가한 `CommentCreatedEvent`가 본 PR에서 실제로 소비됩니다.
- 부수효과로 `CommentControllerTest` cleanup에 알림 정리 추가(댓글 생성이 알림을 남겨 member 삭제 FK 위반 방지).
- 극단적 동시성에서 중복행 삭제 경쟁으로 best-effort 리스너가 예외를 삼켜 로그 노이즈가 날 수 있으나 **1행 수렴은 보장**(MVP 수용, #138 문서화 범위).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/149
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/138
