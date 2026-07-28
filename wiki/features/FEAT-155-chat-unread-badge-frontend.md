---
id: FEAT-155
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [chat, frontend]
pr: 155
---

## 무엇을 / 왜

권건우님이 완료한 채팅 안읽음·읽음 처리 백엔드(PR1c)를 프론트에 연동했다.
채팅방 목록에 안읽은 메시지 수를 배지로 표시하고, 방에 들어가면 자동으로 읽음 처리되도록 구현했다.

## 어떻게 (구현 요약)

- 채팅방 목록(`chat/page.tsx`)의 `ChatRoom` 타입에 `unreadCount` 필드 추가,
카드에 빨간 배지 렌더링 (0건이면 숨김, 99건 초과 시 "99+" 표시)
- 채팅방 상세(`chat/[roomId]/page.tsx`)에 `markRead()` 헬퍼 추가,
최초 로드 완료 직후와 폴링 중 새 메시지 감지 시 두 지점에서 `POST /api/chat-rooms/{roomId}/read` 호출
(fire-and-forget, 실패해도 화면엔 영향 없음)
- 목록 카드에서 마지막 메시지 시각과 안읽음 배지를 같은 오른쪽 정렬 열(`flex-direction: column; align-items: flex-end`)에
배치해 닉네임과 같은 줄 높이에서 시작하도록 레이아웃 조정 (기존에는 시간이 배지보다 왼쪽으로 어긋나 보이던 문제 수정)

**검증**

- 로컬 백엔드(Docker MySQL + Spring Boot) 기동 후 실제 계정 2개로 종단 시나리오 테스트
  1. 구매자 계정으로 상품 채팅 시작 → 메시지 전송
  2. 판매자 계정으로 로그인 → 채팅 목록에서 빨간 배지 "1" 정상 표시 확인
  3. 방 입장 → `POST /api/chat-rooms/{roomId}/read` 200 정상 호출 확인
  4. 목록 복귀 → 배지 정상 소멸 확인
- `npx tsc --noEmit` / `npm run build` 통과

## 건드린 파일

- `frontend/src/app/chat/[roomId]/page.tsx` (+6/-0)
- `frontend/src/app/chat/page.module.css` (+20/-1)
- `frontend/src/app/chat/page.tsx` (+9/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 담당 패키지(report) 외 파일(chat)을 수정했다 — 권건우님이 작성한 프론트 연동 핸드오프 문서를 바탕으로 진행한 작업이다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/155
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/154
