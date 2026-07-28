---
id: FEAT-153
type: feature
status: done
author: horangnabi97
date: 2026-07-06
related: []
tags: [auth, global, backend, docs]
pr: 153
---

## 무엇을 / 왜

비밀번호 재설정 기능 추가

## 어떻게 (구현 요약)

**작업 내용**: 비밀번호 찾기(재설정) 기능 추가. 이메일로 재설정 링크(`http://localhost:3000/password-reset?token={token}`)를 발송하고, 사용자가 링크의 token과 새 비밀번호로 `/confirm` API를 호출해 비밀번호를 재설정하는 흐름.

**담당 도메인**: Auth (김대연)

**변경 파일**
- `auth/controller/PasswordResetController.java` (신규)
- `auth/service/PasswordResetService.java` (신규)
- `auth/entity/PasswordResetToken.java` (신규)
- `auth/repository/PasswordResetTokenRepository.java` (신규)
- `auth/dto/PasswordResetRequest.java`, `PasswordResetConfirmRequest.java` (신규)
- `global/exception/ErrorCode.java` — AUTH 도메인 영역에 `AUTH_015`(INVALID_RESET_TOKEN), `AUTH_016`(EXPIRED_RESET_TOKEN) 추가
- `global/security/SecurityConfig.java` — `/api/auth/password-resets`, `/api/auth/password-resets/confirm` permitAll 추가
- `member/entity/Member.java` — `changePassword(encodedPassword)` 추가
- `application.yml` — `auth.frontend-base-url` 추가(재설정 링크의 프론트 주소, 배포 시 실제 도메인으로 덮어씀)
- `docs/postman/auth-password-reset.md` (신규)

**검증**

- [x] 빌드 성공 / 서버 정상 기동 / 공통 응답 형식 준수
- [x] 재설정 링크 클릭 → 새 비밀번호 설정 → 새 비밀번호로 로그인 성공 / 기존 비밀번호로 로그인 실패 확인
- [x] 동일 token 재사용 시 차단(`INVALID_RESET_TOKEN`) 확인

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/controller/PasswordResetController.java` (+44/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/PasswordResetConfirmRequest.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/PasswordResetRequest.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/auth/entity/PasswordResetToken.java` (+89/-0)
- `backend/src/main/java/com/dongnemarket/auth/repository/PasswordResetTokenRepository.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/auth/service/PasswordResetService.java` (+132/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+1/-1)
- `backend/src/main/resources/application.yml` (+2/-0)
- `backend/src/test/java/com/dongnemarket/auth/controller/PasswordResetControllerTest.java` (+183/-0)
- `backend/src/test/java/com/dongnemarket/auth/service/PasswordResetServiceTest.java` (+262/-0)
- `docs/postman/auth-password-reset.md` (+142/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/153
- 이슈: 없음
