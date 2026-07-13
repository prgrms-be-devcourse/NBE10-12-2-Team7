---
id: FEAT-205
type: feature
status: done
author: horangnabi97
date: 2026-07-09
related: []
tags: [프론트엔드, 게시글, 프로필, 신고, 폴링]
pr: 205
---

## 무엇을 / 왜
프론트엔드 UX 개선 8건을 한 브랜치에 묶어 반영했다. 상품 목록 자동 갱신, 브랜드 리소스 교체, 판매자/상품 표시를 ID→사람이 읽는 값으로, 프로필 편집 UX 분리, 그리고 F5 새로고침 시 내 동네 필터가 비던 인증 타이밍 버그 수정까지 포함한다.

## 어떻게 (구현 요약)
- **상품 목록 5초 폴링**: SSE/WebSocket 없이 기존 조회 API를 재사용해 첫 페이지만 조용히(스피너 없이) 교체. "더보기"로 불러온 추가 페이지는 유지.
- **브랜드 리소스**: OG 공유 카드(카카오톡/슬랙 미리보기)·파비콘을 ISMS 제공 이미지로 교체, 랜딩 히어로에 scroll-snap, 헤더 로고 클릭 경로 `/products` → `/`.
- **판매자/상품 표시**: 상품 상세 판매자를 "회원 #4" → 실제 닉네임+아바타 이니셜(백엔드 `ProductResponse`에 `sellerNickname` 추가). 신고 화면은 "상품 #14" → 실제 상품명(조회 실패 시 안내 문구).
- **프로필 닉네임 편집**: 보기/편집 모드 분리 — 변경 있을 때만 저장 활성화, 취소 시 원복.
- **F5 인증 타이밍 버그**: 새로고침 직후 `AuthBootstrap`의 조용한 재로그인이 끝나기 전에 로그인 여부를 판단해 내 동네 필터가 비던 문제를, 재로그인 완료를 기다린 뒤 판단하도록 수정(추가 네트워크 호출 없음).

## 건드린 파일
- `frontend/src/app/products/page.tsx`, `frontend/src/app/products/[id]/page.tsx` (폴링·판매자 닉네임)
- `frontend/src/app/report/page.tsx` (상품명 표시)
- `frontend/src/app/my-profile/page.tsx` (편집 모드)
- `frontend/src/app/layout.tsx`, `frontend/src/app/page.module.css`, `frontend/src/components/Header.tsx` (OG·스크롤스냅·로고링크)
- `frontend/public/og-image.png`, `frontend/src/app/favicon.ico` (브랜드 리소스)
- `backend/src/main/java/com/dongnemarket/product/dto/ProductResponse.java` (`sellerNickname` 필드)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (검증 추가)

## 결정과 트레이드오프
- 실시간 갱신에 SSE/WebSocket 대신 **폴링** 선택 — 기존 조회 API 재사용으로 서버 변경 없이 즉시 도입. 대신 주기적 요청 비용을 감수(초기 7초 → 5초로 조정, `d5588ef`).
- PR 스스로 인정하듯 **성격이 다른 UX 수정 8건이 한 PR에 누적**됐다("담당 패키지 외 수정 없음" 원칙과도 겹침 — 백엔드 `product` 파일 포함). 리뷰/추적성 측면에서 다음엔 분리 권장.

## 남은 이슈 / 후속 작업
- (프로세스) 서로 다른 관심사는 PR을 쪼갤 것 — 이 PR이 반례 사례.

## 링크
- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/205
- 이슈: 해당 없음
