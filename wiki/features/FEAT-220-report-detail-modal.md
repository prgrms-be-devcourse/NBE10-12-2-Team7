---
id: FEAT-220
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-23
related: []
tags: [신고, 프론트엔드, 모달, my-reports]
pr: 220
---

## 무엇을 / 왜

신고 상세 조회 API(`GET /api/members/me/reports/{reportId}`)는 이미 구현돼 있었는데, 이를 호출하는 화면이 없어서 **실제로 아무도 호출하지 않는 죽은 API** 상태였다. 신고내역 페이지에서 상세 내용을 확인할 수 있게 화면을 붙였다.

## 어떻게 (구현 요약)

- `ReportDetailModal` 컴포넌트를 신규 작성.
- 신고내역(`my-reports`) 페이지의 각 신고 카드에 "상세보기" 버튼을 추가하고, 클릭 시 모달을 연다.
- 모달에 신고번호 / 신고 대상 / 신고 사유 / 처리 상태 / 접수일 / 상세 내용(`content`) / 증빙 이미지를 표시.

## 건드린 파일

- `frontend/src/components/ReportDetailModal.tsx` (+87/-0)
- `frontend/src/components/ReportDetailModal.module.css` (+96/-0)
- `frontend/src/app/my-reports/page.tsx` (+27/-3)
- `frontend/src/app/my-reports/page.module.css` (+14/-0)

## 결정과 트레이드오프

- 별도 상세 **페이지**가 아니라 **모달**로 구현 — 신고내역 목록에서 맥락을 잃지 않고 확인할 수 있고, 라우팅 추가가 필요 없다.
- 연동 API는 기존 것을 그대로 사용했고 백엔드 변경은 없다.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/220
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/219
