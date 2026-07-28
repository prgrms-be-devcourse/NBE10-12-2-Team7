---
id: FEAT-107
type: feature
status: done
author: Crispy-down
date: 2026-07-03
related: [FEAT-120, FEAT-141]
tags: [chat, global, backend]
pr: 107
---

## 무엇을 / 왜

1:1 채팅 코어 골격 — 엔티티·Repository·ErrorCode (PR1a)

## 어떻게 (구현 요약)

1:1 채팅(폴링 기반)의 **저장·조회 골격**(PR1a). 서비스/컨트롤러/REST/테스트는 후속 PR1b.
- `ChatRoom` 엔티티 — `product`/`buyer`/`seller` @ManyToOne(LAZY), **UNIQUE(product_id, buyer_id)**
- `ChatMessage` 엔티티 — `chatRoom`/`sender` + `content(1000)`
- `ChatRoomRepository` — get-or-create 조회(`findByProduct_IdAndBuyer_Id`) + 내 방 목록(product/buyer/seller fetch join)
- `ChatMessageRepository` — id 기반 커서 페이지네이션(`findPageByRoom`, sender fetch join)
- `ErrorCode` CHAT 섹션: `CHAT_ROOM_NOT_FOUND`(404)/`CHAT_ACCESS_DENIED`(403)/`CANNOT_CHAT_WITH_SELF`(400)

**검증**

- 골격 PR이라 단위/통합 테스트는 **PR1b**에서 (서비스·컨트롤러와 함께).
- 대신 다음으로 검증: `./gradlew compileJava` BUILD SUCCESSFUL + `@SpringBootTest` 컨텍스트 부팅으로 **새 Repository `@Query` JPQL 파싱·검증**(멀티 fetch join, `(:cursor IS NULL OR m.id < :cursor)`) 및 chat 테이블 DDL 생성 확인. 전체 스위트 회귀 없음.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/entity/ChatMessage.java` (+57/-0)
- `backend/src/main/java/com/dongnemarket/chat/entity/ChatRoom.java` (+77/-0)
- `backend/src/main/java/com/dongnemarket/chat/repository/ChatMessageRepository.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/chat/repository/ChatRoomRepository.java` (+33/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+5/-0)

## 결정과 트레이드오프

- **방 중복 방지 = UNIQUE(product_id, buyer_id)** → 방 생성은 get-or-create(멱등). 동시 생성은 DB 제약이 하나만 통과(Favorite race 처리와 동일 패턴을 PR1b에서 재사용).
- **seller를 방에 저장(비정규화)** — 참여자 인가가 매 메시지 조회·전송마다 실행되므로, 파생(`product.member`) 시 인가마다 Product 로딩(N+1)이 붙는다. 스냅샷 저장해 인가/필터를 순수 row 비교로 유지. 상품 소유권 불변이라 드리프트 없음.
- **커서 페이지네이션 + id 단독 커서** — 메시지는 무한 성장/조회 중 삽입되므로 offset은 느려지고 페이지 경계가 밀려 중복·누락 발생. `id`가 IDENTITY라 단조증가 → id 하나로 완전한 정렬키(created_at 타이브레이크 불필요).
- **대화 기록 보존** — 상품 삭제·숨김돼도 방/메시지 유지(cascade 삭제 없음). 접근성 검증은 방 생성 시에만.

## 남은 이슈 / 후속 작업

- **엔티티만 있는 골격 PR** — 이 PR 단독으로는 노출 엔드포인트가 없다(테이블/모델만 추가). 실제 API는 PR1b.
- **seller 비정규화 저장**은 정규화보다 인가 성능을 택한 의도적 결정 — 리뷰 시 위 근거 참고.
- `ddl-auto: update` 환경이라 머지 시 chat_rooms/chat_messages 테이블이 자동 생성됨.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/107
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/106
