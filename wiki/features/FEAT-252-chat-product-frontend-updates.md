---
id: FEAT-252
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-27
related: []
tags: [chat, products, frontend]
pr: 252
---

## 무엇을 / 왜

권건우님 BE 핸드오프 문서 기준으로, 이미 준비된 백엔드 신호(탈퇴 여부, 자기 상품 찜 차단) 2가지를 프론트엔드에 연동했다.

Closes #251

## 어떻게 (구현 요약)

**탈퇴 상대 채팅 UX**
- `chat/[roomId]/page.tsx`: `ChatMemberSummary`에 `withdrawn` 필드 추가,
   `opponent.withdrawn`이면 입력창·전송 버튼 비활성화 + "탈퇴한 사용자와는 더 이상 대화할 수 없습니다." 배너 노출
- `chat/page.tsx`(목록, 선택 항목): 상대 닉네임 옆에 "탈퇴" 뱃지 표시

**자기 상품 찜 버튼 가드**
- `products/[id]/page.tsx`: 찜(하트) 버튼을 채팅 버튼과 동일하게 `!isOwner`일 때만 렌더
   — 본인 상품에 찜을 시도해 400(`CANNOT_FAVORITE_OWN_PRODUCT`)이 나던 과도기 제거

**검증**

- `npm run lint` + `npm run build` 통과
- 실 서버로 실제 시나리오 재현: 채팅방 생성 → 구매자 탈퇴 처리 →
  판매자 계정으로 목록/상세 화면에서 배너·뱃지·입력창 비활성화 확인
- 비소유자 계정으로 찜 버튼이 정상 노출되는 것(회귀 없음)까지 확인

## 건드린 파일

- `frontend/src/app/chat/[roomId]/page.module.css` (+12/-0)
- `frontend/src/app/chat/[roomId]/page.tsx` (+14/-1)
- `frontend/src/app/chat/page.module.css` (+9/-0)
- `frontend/src/app/chat/page.tsx` (+2/-0)
- `frontend/src/app/products/[id]/page.tsx` (+13/-11)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

권건우님이 최근 추가한 다른 백엔드 기능(찜한 사용자 가격 변경 알림, 댓글 작성자 닉네임, 관심목록 썸네일 등)도 전수 조사했는데,
전부 기존에 이미 구현된 화면(알림 패널의 `PRICE_CHANGE` 타입 처리, 상품상세 댓글 닉네임 표시 등)으로 커버되고 있어서
이번 PR의 2가지 외에 추가로 필요한 화면은 없었다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/252
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/251
