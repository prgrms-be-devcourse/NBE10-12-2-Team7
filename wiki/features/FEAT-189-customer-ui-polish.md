---
id: FEAT-189
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-07
related: []
tags: [chat, favorites, find-password, login, my-products, my-profile]
pr: 189
---

## 무엇을 / 왜

고객 화면 API 힌트 제거 및 로그인/회원가입 인트로 폰트 확대

## 어떻게 (구현 요약)

- 회원가입/로그인/상품/채팅/신고 등 고객용 화면 15곳에 남아있던 "GET/POST /api/..." 형태의 API 힌트 문구를 전부 제거했다.
- report 페이지의 apiEndpoint처럼 힌트 표시에만 쓰이던 미사용 변수도 함께 정리했다.
- 로그인/회원가입 페이지의 인트로 배지·제목·설명 문구 폰트 크기를 키웠다.

**검증**

- [x] 빌드 성공 / 서버 정상 기동 (npm run lint / npx tsc --noEmit / npm run build 모두 통과)
- [x] 담당 API 정상 동작 — API 변경 없음, 화면 렌더링만 확인
- [x] develop 최신 반영 / 충돌 해결 (AgreementSection 등 팀원 신규 작업과 merge 충돌 없이 반영됨 확인)
- [ ] 공통 응답 형식(ApiResponse / ErrorResponse) 준수 — 해당 없음

## 건드린 파일

- `frontend/src/app/chat/[roomId]/page.module.css` (+0/-10)
- `frontend/src/app/chat/[roomId]/page.tsx` (+0/-2)
- `frontend/src/app/chat/page.module.css` (+0/-9)
- `frontend/src/app/chat/page.tsx` (+0/-2)
- `frontend/src/app/favorites/page.module.css` (+0/-5)
- `frontend/src/app/favorites/page.tsx` (+0/-4)
- `frontend/src/app/find-password/page.module.css` (+0/-1)
- `frontend/src/app/find-password/page.tsx` (+0/-1)
- `frontend/src/app/login/page.module.css` (+3/-4)
- `frontend/src/app/login/page.tsx` (+0/-1)
- `frontend/src/app/my-products/page.module.css` (+0/-5)
- `frontend/src/app/my-products/page.tsx` (+0/-4)
- `frontend/src/app/my-profile/page.module.css` (+0/-8)
- `frontend/src/app/my-profile/page.tsx` (+0/-4)
- `frontend/src/app/my-reports/page.module.css` (+0/-9)
- … 외 13개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/189
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/188
