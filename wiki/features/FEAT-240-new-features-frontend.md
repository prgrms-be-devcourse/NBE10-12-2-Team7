---
id: FEAT-240
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-27
related: []
tags: [admin, escrow, globals, layout, products, frontend]
pr: 240
---

## 무엇을 / 왜

프론트엔드 미구현 백엔드 기능을 전수 조사해서 찾은 4가지를 모두 연동했다.
백엔드 코드는 전혀 건드리지 않고 프론트엔드에서만 작업했다.

Closes #239

## 어떻게 (구현 요약)

**안심결제(에스크로)**
- 상품상세 페이지에 "안심결제로 구매하기" 버튼 추가(채팅으로 거래하기 옆)
- 거래 상태 페이지(`/escrow/[escrowId]`) 신규 — 구매확정/거래취소 액션 포함
- 백엔드에 "내 진행중인 에스크로 조회" API가 없어서, escrowId를 상품별로 localStorage에 임시 보완해 재진입 가능하게 함

**관리자 - 저신뢰 회원 조회**
- `/admin/manner-scores` 신규 — 임계치 필터, 회원 상세로 바로가기

**관리자 - 저장소 고아파일 정리**
- `/admin/storage` 신규 — 고아파일 목록 조회, 체크박스 선택 삭제

**거래 법률 상담 + 실시간 딜**
- `LegalChatWidget` 신규 — 전 페이지 우측 하단 플로팅 노출(관리자 화면 제외), `/agent/legal/ask` 연동
- `next.config.ts`에 `/agent/*` → Python agent 서비스 프록시 라우트 추가
- 상단 네비게이션에 "실시간 딜" 버튼 추가(백엔드 미구현이라 클릭해도 아무 동작 안 함, 디자인만 우선 반영)

**검증**

- 안심결제: 실 서버로 상품 등록 → 구매 → 에스크로 생성 → 구매확정까지 라이브로 전체 흐름 확인, 상품 상태(ON_SALE→RESERVED→COMPLETED) 전환도 함께 확인
- 관리자 저신뢰 회원 조회: 실제 저신뢰 데이터로 목록 조회·임계치 필터 확인
- 관리자 저장소 정리: 실제 고아파일 128개로 조회 확인, curl로 삭제 API까지 직접 호출해 정상 삭제(개수 128→127) 확인
- 거래 법률 상담: 실제 `agent/` 서비스는 NVIDIA NIM API 키가 없어 기동이 불가능해서,
  코드에서 확인한 실제 요청/응답 계약을 그대로 복제한 임시 목 서버로 화면 렌더링(질문·답변·출처)까지 확인.
  실제 서비스가 준비되면 코드 수정 없이 그대로 붙는 구조.

## 건드린 파일

- `frontend/next.config.ts` (+6/-0)
- `frontend/src/app/admin/layout.tsx` (+2/-0)
- `frontend/src/app/admin/manner-scores/page.tsx` (+103/-0)
- `frontend/src/app/admin/storage/page.tsx` (+220/-0)
- `frontend/src/app/escrow/[escrowId]/page.module.css` (+112/-0)
- `frontend/src/app/escrow/[escrowId]/page.tsx` (+195/-0)
- `frontend/src/app/globals.css` (+5/-0)
- `frontend/src/app/layout.tsx` (+2/-0)
- `frontend/src/app/products/[id]/page.module.css` (+11/-0)
- `frontend/src/app/products/[id]/page.tsx` (+51/-0)
- `frontend/src/components/Header.tsx` (+20/-5)
- `frontend/src/components/LegalChatWidget.module.css` (+175/-0)
- `frontend/src/components/LegalChatWidget.tsx` (+170/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 거래 법률 상담은 실제 `agent/` 서비스(NIM API 키 필요)로 아직 라이브 검증은 못 했다. 키가 준비되면 한 번 더 확인 필요.
- 실시간 딜은 백엔드 기능 자체가 없어서 버튼 디자인만 우선 반영했고, 기능 구현 후 화면을 이어서 붙일 예정.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/240
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/239
