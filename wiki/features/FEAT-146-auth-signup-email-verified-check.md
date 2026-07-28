---
id: FEAT-146
type: feature
status: done
author: horangnabi97
date: 2026-07-06
related: [FEAT-147]
tags: [auth, global, member, backend, docs]
pr: 146
---

## 무엇을 / 왜

회원가입 이메일 인증 기능 추가

## 어떻게 (구현 요약)

- 회원가입 전 이메일 인증 기능 추가
- Gmail SMTP 기반 메일 발송 인프라 추가
- 인증코드 발송 API 추가
- 인증코드 확인 API 추가
- 회원가입 시 이메일 인증 완료 여부 검증 연동

관련 작업(비밀번호 정책 강화)은 별도 PR 예정입니다.

**검증**

- `./gradlew clean test` **405개 통과** (0 failed, 0 skipped)
- 실제 MySQL + Postman 검증 완료 (`docs/postman/auth-email-verification.md`, `docs/postman/auth-signup.md`)
- 실제 Gmail SMTP로 메일 발송 확인
- 민감정보는 `.env`로만 관리했고 커밋에 포함되지 않음

이 PR은 이메일 인증 기능 전체를 하나의 기능 단위로 묶었습니다.

## 건드린 파일

- `.env.example` (+3/-0)
- `.gitignore` (+4/-0)
- `backend/bin/main/application.yml` (+0/-40)
- `backend/bin/test/application-test.yml` (+0/-18)
- `backend/build.gradle` (+1/-0)
- `backend/src/main/java/com/dongnemarket/auth/controller/EmailVerificationController.java` (+46/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/EmailVerificationConfirmRequest.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/EmailVerificationConfirmResponse.java` (+20/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/EmailVerificationRequest.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/EmailVerificationResponse.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/auth/entity/EmailVerification.java` (+111/-0)
- `backend/src/main/java/com/dongnemarket/auth/mail/EmailSender.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/auth/mail/SmtpEmailSender.java` (+42/-0)
- `backend/src/main/java/com/dongnemarket/auth/repository/EmailVerificationRepository.java` (+14/-0)
- `backend/src/main/java/com/dongnemarket/auth/service/AuthService.java` (+8/-1)
- … 외 16개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- `SecurityConfig` permitAll 경로 추가 (`/api/auth/email-verifications`, `/api/auth/email-verifications/confirm`)
- `build.gradle`에 `spring-boot-starter-mail` 의존성 추가
- `docker-compose.yml` `app` 서비스에 메일 환경변수(`MAIL_HOST/PORT/USERNAME/PASSWORD`) 추가
- `.env.example`에 메일 환경변수 키 문서화(플레이스홀더만, 실제 값 아님)

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/146
- 이슈: 없음
