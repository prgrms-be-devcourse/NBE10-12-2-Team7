---
id: FEAT-123
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-03
related: []
tags: [chat, my-profile, products, frontend]
pr: 123
---

## 무엇을 / 왜

develop에 새로 merge된 백엔드 채팅(1:1 채팅 REST API)·지역(Region) 도메인을 프론트엔드에 연동했다.
상품 상세의 "채팅 기능은 준비 중이에요" 플레이스홀더를 실제 채팅으로 교체하고,
내 정보 페이지에 동네 설정 UI를 추가하고,
상품 등록/수정 폼의 자유 텍스트 지역 입력을 지역 마스터 목록 기반 select로 교체했다.

## 어떻게 (구현 요약)

- [x] `/chat` : 내 채팅방 목록 페이지 신규 구현
- [x] `/chat/[roomId]` : 채팅방 페이지 신규 구현 (메시지 조회/전송, 3초 폴링으로 새 메시지 반영, 내 메시지/상대 메시지 말풍선 구분)
- [x] 상품 상세 페이지 "채팅으로 거래하기" 버튼을 실제 API 연동으로 교체 (판매자 본인 상품에는 버튼 숨김)
- [x] 헤더 네비게이션에 "채팅" 메뉴 추가
- [x] 내 정보 페이지에 "내 동네 설정" 카드 추가 (동네 추가/삭제, 대표 동네 지정, 전체 교체 저장)
- [x] ProductForm의 거래 지역 입력을 자유 텍스트 → 지역 마스터 목록 기반 select로 교체 (수정 화면에서 마스터 목록에 없는 기존 값은 임시 옵션으로 보존)

**검증**

프론트엔드 전용 작업이며 대상 API들은 팀원이 이미 테스트 코드로 검증 완료한 상태이다.
로컬 백엔드(Docker) 미기동 환경에서 작업하여 실제 DB 연동 테스트는 진행하지 못했고,
브라우저에서 API 응답을 mocking해 화면 렌더링·상태 전이·에러 처리를 확인했다.
추가로 `tsc --noEmit`, `npm run build` 통과를 확인했다.

## 건드린 파일

- `frontend/src/app/chat/[roomId]/page.module.css` (+163/-0)
- `frontend/src/app/chat/[roomId]/page.tsx` (+222/-0)
- `frontend/src/app/chat/page.module.css` (+84/-0)
- `frontend/src/app/chat/page.tsx` (+126/-0)
- `frontend/src/app/my-profile/page.module.css` (+67/-0)
- `frontend/src/app/my-profile/page.tsx` (+145/-0)
- `frontend/src/app/products/[id]/page.tsx` (+23/-3)
- `frontend/src/components/Header.tsx` (+1/-0)
- `frontend/src/components/ProductForm.tsx` (+19/-7)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 채팅방 상세 진입 시 상대방/상품 정보는 별도의 room 단건 조회 API가 없어, `GET /api/chat-rooms` 목록에서 roomId로 찾아 표시하는 방식을 사용했다.
- 백엔드 코드는 수정하지 않았다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/123
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/122
