---
id: FEAT-171
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [frontend]
pr: 171
---

## 무엇을 / 왜

프론트엔드 전체를 점검하다가 발견한 미연동 항목을 처리했다.
`hasUnreadNotification`이 항상 `false`로 고정되어 있었는데, 최근 백엔드에 알림 도메인이 추가되면서
연동 가능한 API가 이미 존재했다.

## 어떻게 (구현 요약)

- 로그인 상태일 때 `GET /api/notifications/unread-count`를 호출해서 `unreadCount > 0` 여부로 배지를 표시하도록 연동했다.
- 20초 주기로 폴링하고, 로그인/로그아웃 시(`AUTH_CHANGED_EVENT`)에도 재조회하도록 했다.

**검증**

- API가 처음엔 500 에러를 반환했는데, 로컬에 떠 있던 백엔드 프로세스가 알림 기능 merge 전 코드로 오래 실행 중이던 게 원인이었다. 최신 코드로 재기동하니 정상 동작했다.
- 실제 계정 두 개로 댓글 알림을 발생시켜 배지가 뜨는 것, 읽음 처리(`POST /api/notifications/read`) 후
배지가 사라지는 것까지 브라우저에서 확인했다.
- 알림이 없는 초기 상태에서 배지가 안 뜨는 것도 확인했다.

## 건드린 파일

- `frontend/src/components/Header.tsx` (+24/-4)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 같이 검토했던 `products` 페이지 배너 통계(`STAT_TARGETS` 하드코딩)는 실제 집계가 아니라 연출용 수치라
API 연동 없이 그대로 둔다.
- 담당 패키지(report) 외 파일(공용 `Header.tsx`)을 수정했다 — 팀에서 요청한 미연동 항목 점검 작업이다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/171
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/170
