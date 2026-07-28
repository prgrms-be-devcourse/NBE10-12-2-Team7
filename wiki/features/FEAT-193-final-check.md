---
id: FEAT-193
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-07
related: []
tags: [notification, admin, chat, favorites, find-password, globals]
pr: 193
---

## 무엇을 / 왜

최종 점검 — 버그 수정, 신고내역 통계 UI, 댓글 닉네임/알림 문구 연동

## 어떻게 (구현 요약)

- CSS Modules에서 animation-name도 스코프 해시가 붙어 globals.css의 전역 keyframes(mo-rise/mo-pop/mo-wave/mo-pulse)와 이름이 안 맞아 애니메이션이 조용히 무효화되던 문제를 14개 파일에서 로컬 재선언으로 수정했다.
- 상품목록 배너의 펄스 애니메이션 강도·속도, 물결 그라데이션 애니메이션을 원래 디자인 값으로 복원했다.
- 헤더 배경에 반투명 블러 효과를 복원하고, 누락됐던 --warm-soft 색상 변수를 추가했다.
- 헤더 버튼·배너·검색창·카테고리 칩 등 세로 여백을 줄였다.
- 상품 상세 페이지 썸네일이 2장일 때 과도하게 커지던 문제를 고정 크기(80px)로 전환해서 수정했다.
- 이용약관 등 정책 문서 목록의 들여쓰기를 제목과 맞췄다.
- "내신고내역" 페이지에 상태별 도넛 차트, 가장 많이 접수한 사유, 신고 유형 비율, 상태 진행 스테퍼를 추가했다.
- 관리자 페이지에도 사용자 페이지와 동일한 라이트/다크 모드 버튼을 추가했다.
- 관리자 페이지 9곳에 노출되어 있던 API 경로 문구를 제거했다.
- 댓글 목록에 작성자 닉네임(탈퇴 사용자는 "탈퇴한 사용자"로 마스킹)을 연동했다.
- 알림 문구 맨 앞에 타입별 이모지를 추가했다(💬 댓글 · 🏷️ 가격변경 · 🔔 채팅).

**검증**

- [x] 빌드 성공 / 서버 정상 기동 (npm run lint / npx tsc --noEmit / npm run build, 백엔드 compileJava 및 관련 단위 테스트 모두 통과)
- [x] 담당 API 정상 동작 — 컴파일된 결과물을 실제 서버에서 직접 받아 애니메이션·통계 UI 반영 확인
- [x] develop 최신 반영 / 충돌 해결
- [ ] 공통 응답 형식(ApiResponse / ErrorResponse) 준수 — 해당 없음(응답 포맷 변경 없음)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/notification/service/NotificationService.java` (+3/-3)
- `frontend/src/app/admin/ai/page.tsx` (+1/-1)
- `frontend/src/app/admin/comments/page.tsx` (+1/-1)
- `frontend/src/app/admin/dashboard/page.tsx` (+1/-1)
- `frontend/src/app/admin/layout.tsx` (+47/-0)
- `frontend/src/app/admin/members/[id]/page.tsx` (+0/-1)
- `frontend/src/app/admin/members/page.tsx` (+1/-1)
- `frontend/src/app/admin/products/[id]/page.tsx` (+0/-1)
- `frontend/src/app/admin/products/page.tsx` (+1/-1)
- `frontend/src/app/admin/reports/[id]/page.tsx` (+0/-1)
- `frontend/src/app/admin/reports/page.tsx` (+1/-1)
- `frontend/src/app/chat/page.module.css` (+7/-0)
- `frontend/src/app/favorites/page.module.css` (+7/-0)
- `frontend/src/app/find-password/page.module.css` (+10/-3)
- `frontend/src/app/globals.css` (+13/-19)
- … 외 14개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/193
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/192
