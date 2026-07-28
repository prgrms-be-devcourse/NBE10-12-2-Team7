---
id: FEAT-116
type: feature
status: done
author: horangnabi97
date: 2026-07-03
related: []
tags: [auth, backend, docs]
pr: 116
---

## 무엇을 / 왜

- 인증된 사용자가 명시적으로 세션을 종료할 수 있는 로그아웃 API(`POST /api/auth/logout`)를 추가했습니다.
- global 패키지 변경 없이 auth 패키지 내부 작업만으로 완료했습니다.

## 어떻게 (구현 요약)

- `POST /api/auth/logout` 추가 — `Authorization: Bearer {accessToken}` 인증 필요, 요청 본문 없음
- 로그아웃 시 해당 회원의 저장된 Refresh Token을 DB에서 삭제 (`RefreshTokenRepository.deleteByMemberId`, `RefreshTokenService.deleteByMemberId`)
- 로그아웃은 **멱등**하게 동작 — 이미 삭제된 상태에서 다시 호출해도 항상 200
- Access Token 자체는 즉시 무효화하지 않음(Stateless JWT 정책 유지) — 로그아웃으로 막히는 것은 "그 세션으로 새 Access Token을 재발급받는 것"이며, 이미 발급된 Access Token은 만료 시각(최대 15분)까지 그대로 유효함
- `AuthService`는 `RefreshTokenRepository`를 직접 참조하지 않고 `RefreshTokenService`만 의존 — 저장소를 DB에서 Redis 등으로 교체하더라도 `RefreshTokenService` 내부만 바뀌면 되는 구조

**검증**

| 시나리오 | 결과 |
|---|---|
| 로그인한 사용자가 로그아웃 | 200 성공 |
| 로그아웃을 여러 번 연속 호출 | 매번 200 (멱등) |
| 인증 헤더 없이 로그아웃 시도 | 401 `UNAUTHORIZED` |
| 로그아웃 후 기존 Refresh Token으로 재발급 시도 | 401 `REFRESH_TOKEN_NOT_FOUND` (신규 ErrorCode 아님, 기존 코드 재사용) |
| 로그아웃 후에도 기존 Access Token으로 다른 보호 API 호출 | 200 성공 (Stateless 정책에 따른 의도된 동작) |
| 관리자 계정(`ROLE_ADMIN`)으로 동일 시나리오 | 일반 사용자와 동일하게 전부 동작 확인 |

모든 항목은 로컬 MySQL + `bootRun`으로 실제 curl 요청까지 실측했습니다 (`docs/postman/auth-logout.md` 참고).

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java` (+8/-0)
- `backend/src/main/java/com/dongnemarket/auth/repository/RefreshTokenRepository.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/auth/service/AuthService.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/auth/service/RefreshTokenService.java` (+11/-1)
- `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java` (+69/-0)
- `backend/src/test/java/com/dongnemarket/auth/service/AuthServiceTest.java` (+39/-0)
- `backend/src/test/java/com/dongnemarket/auth/service/RefreshTokenServiceTest.java` (+20/-0)
- `docs/postman/auth-logout.md` (+147/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 자동 로그인, 이메일 인증, 비밀번호 변경
- Redis 기반 저장소 전환 (현재는 DB 기반 구조만 사용)

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/116
- 이슈: 없음
