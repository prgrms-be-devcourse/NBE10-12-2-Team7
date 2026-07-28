---
id: FEAT-121
type: feature
status: done
author: horangnabi97
date: 2026-07-03
related: []
tags: [auth, admin, favorites, layout, login, my-products]
pr: 121
---

## 무엇을 / 왜

Refresh Token HttpOnly Cookie 전환 및 자동 로그인 구현

## 어떻게 (구현 요약)

**담당 도메인**: Auth · Member

**작업 내용**
- Refresh Token(7일)을 HttpOnly Cookie로, Access Token(15분)은 프론트 메모리 변수로만 관리하도록 전환 (XSS로 두 토큰이 함께 탈취되는 위험 제거)
- 로그인 화면에 "자동 로그인" 체크박스 추가 — 체크 시 영속 쿠키(Max-Age 7일), 미체크 시 세션 쿠키
- JwtAuthenticationFilter/JwtTokenProvider/SecurityConfig/AuthService/RefreshTokenService는 변경 없이 AuthController에서만 쿠키 처리
- 리베이스 중 develop에 먼저 병합된 admin/layout.tsx가 이번 변경으로 깨지는 것을 발견해 함께 수정했습니다.

**변경 파일**
- Backend: `auth/controller/AuthController.java`, `auth/dto/{AccessTokenResponse,LoginRequest}.java`(추가), `ReissueRequest.java`(삭제), `application.yml`, `auth/controller/AuthControllerTest.java`
- Docs: `docs/postman/auth-autologin.md`(신규), `auth-logout.md`, `auth-refresh-token.md`
- Frontend: `lib/auth.ts`, `lib/apiClient.ts`, `login/page.tsx`, `admin/layout.tsx`, `ProductForm.tsx`, `my-products/my-profile/my-reports/favorites/report/products/[id]/page.tsx`, `components/AuthBootstrap.tsx`, `app/layout.tsx`

**검증**

- [x] 빌드 성공 / 서버 정상 기동 / 공통 응답 형식 준수 (`./gradlew test` BUILD SUCCESSFUL, frontend `tsc --noEmit` / `next build` 성공)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java` (+54/-11)
- `backend/src/main/java/com/dongnemarket/auth/dto/AccessTokenResponse.java` (+18/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/LoginRequest.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/ReissueRequest.java` (+0/-20)
- `backend/src/main/resources/application.yml` (+6/-0)
- `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java` (+66/-30)
- `docs/postman/auth-autologin.md` (+135/-0)
- `docs/postman/auth-logout.md` (+16/-13)
- `docs/postman/auth-refresh-token.md` (+98/-57)
- `frontend/src/app/admin/layout.tsx` (+32/-20)
- `frontend/src/app/favorites/page.tsx` (+4/-20)
- `frontend/src/app/layout.tsx` (+2/-0)
- `frontend/src/app/login/page.module.css` (+4/-0)
- `frontend/src/app/login/page.tsx` (+15/-3)
- `frontend/src/app/my-products/page.tsx` (+6/-24)
- … 외 8개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/121
- 이슈: 없음
