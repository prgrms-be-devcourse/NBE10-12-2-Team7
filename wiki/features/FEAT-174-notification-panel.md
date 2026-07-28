---
id: FEAT-174
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-07
related: []
tags: [globals, frontend]
pr: 174
---

## 무엇을 / 왜

프론트엔드 전체를 점검하다가 발견한 미연동 항목을 처리했다.
백엔드에 상품 가격변경(PRICE_CHANGE) 알림이 추가되면서 알림 도메인에 댓글/채팅/가격변경 세 타입이 모두 존재하게 됐는데,
프론트에는 헤더 배지(점)만 있고 실제 알림을 확인할 화면이 없었다. 벨 아이콘에 클릭 핸들러조차 없었다.

## 어떻게 (구현 요약)

- 벨 아이콘 클릭 시 알림 목록 드롭다운 패널을 띄우도록 했다.
- 패널을 열면 `GET /api/notifications`로 목록을 불러오고, `POST /api/notifications/read`로 읽음 처리하도록 했다.
- 알림 타입별로 클릭 시 이동 경로를 분기했다: `CHAT` -> `/chat/{roomId}`, `COMMENT`·`PRICE_CHANGE` -> `/products/{productId}`.
- 바깥 영역을 클릭하면 패널이 자동으로 닫히도록 했다.

**검증**

- 실제 계정 두 개(판매자/구매자)로 채팅방을 개설한 뒤 판매자가 상품 가격을 변경해서,
구매자 쪽 알림 패널에 가격변경 알림이 뜨는 것까지 브라우저에서 확인했다.
- 기존 채팅 알림(`CHAT`), 댓글 알림도 패널에서 정상 표시되고 클릭 시 각각 채팅방/상품 페이지로 이동하는 것을 확인했다.
- 패널을 열람하면 배지(점)가 사라지는 것을 확인했다. 단, 안읽은 채팅이 남아있으면 배지가 다시 뜨는데,
이건 배지가 댓글·가격변경뿐 아니라 안읽은 채팅방 수도 함께 세는 백엔드 설계상 정상 동작이다.
- `npm run lint` / `npx tsc --noEmit` / `npm run build` 모두 통과했다.

## 건드린 파일

- `frontend/src/app/globals.css` (+25/-0)
- `frontend/src/components/Header.tsx` (+90/-8)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 헤더는 별도 CSS 모듈이 없어서 `globals.css`에 스타일을 추가했다.
- 담당 패키지(report) 외 파일(공용 `Header.tsx`, `globals.css`)을 수정했다 — 팀에서 요청한 미연동 항목 점검 작업이다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/174
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/173
