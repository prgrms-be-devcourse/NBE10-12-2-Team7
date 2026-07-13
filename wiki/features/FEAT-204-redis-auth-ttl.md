---
id: FEAT-204
type: feature
status: done
author: horangnabi97
date: 2026-07-09
related: []
tags: [인증, redis, 리프레시토큰, 브루트포스, 보안]
pr: 204
---

## 무엇을 / 왜
Auth 도메인의 TTL(유효기간) 데이터 4종 — Refresh Token, 이메일 인증 코드, 비밀번호 재설정 토큰, 로그인 실패 횟수 — 을 MySQL에서 Redis로 이전했다. 만료 판정·정리를 애플리케이션이 직접 하지 않고 Redis `EXPIRE`에 위임하기 위함. 함께 **Refresh Token Rotation**과 **로그인 실패 횟수 제한(브루트포스 방지)**을 신규 도입했다.

## 어떻게 (구현 요약)
- **리포지토리 인터페이스화 + 프로파일별 Bean 분리**: dev/prod는 Redis 구현체, test는 인메모리 구현체 → `./gradlew test`는 외부 Redis 없이 통과. 진짜 Redis 검증은 Testcontainers(`integration` 태그)로 분리.
- **Refresh Token Rotation**: `/reissue` 성공 시마다 새 토큰을 발급·저장하고 쿠키도 다시 내려보냄. 탈취된 옛 토큰이 재사용되면 저장값과 불일치해 실패 → 재사용 탐지 효과.
- **로그인 실패 제한**: 이메일당 실패 5회 도달 시 10분 차단(`AUTH_019 TOO_MANY_LOGIN_ATTEMPTS`, 429). 탈퇴/정지 회원 거부는 자격증명 추측 신호가 아니므로 카운트하지 않음.
- **Redis 커맨드 타임아웃 2초 명시**(`spring.data.redis.timeout`): 기본값(Lettuce 60초)이면 Redis 장애 시 요청이 1분씩 붙잡혀 톰캣 스레드풀이 고갈될 위험.
- **Flyway V2**로 `email_verifications`의 미사용 컬럼(`code`/`sent_at`/`expires_at`)과 `password_reset_tokens` 테이블 제거.

## 건드린 파일
- `backend/src/main/java/com/dongnemarket/auth/repository/Redis{RefreshToken,LoginAttempt,EmailVerificationCode,PasswordResetToken}Repository.java` (신규, dev/prod)
- `backend/src/main/java/com/dongnemarket/auth/repository/In{RefreshToken,LoginAttempt,EmailVerificationCode,PasswordResetToken}Repository.java` (test 구현체)
- `backend/src/main/java/com/dongnemarket/auth/service/{AuthService,RefreshTokenService,EmailVerificationService,PasswordResetService,LoginAttemptService}.java`
- `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java` (`/reissue` 쿠키 재설정)
- `backend/src/main/java/com/dongnemarket/auth/entity/EmailVerification.java` (컬럼 축소), `PasswordResetToken.java` (삭제 — Redis 2-key로 대체)
- `backend/src/main/resources/db/migration/V2__drop_unused_email_verification_and_password_reset_columns.sql`
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (AUTH_019 추가)
- `docker-compose.yml`, `infra/cloud/app/docker-compose.yml`, `infra/onprem/docker-compose.yml` (redis 서비스)
- `docs/adr/0006-redis-for-auth-ttl-data.md`, `docs/runbook/db-migrations.md` (정본 문서 갱신)

## 결정과 트레이드오프
- **배경·근거의 정본은 [ADR-0006](../../docs/adr/0006-redis-for-auth-ttl-data.md)** — 이 위키 문서는 서사, 결정 정본은 `docs/adr`.
- 프로파일별 구현 분리로 단위 테스트가 외부 인프라에 의존하지 않게 유지. 대신 test/dev/prod에서 서로 다른 Bean이 도는 만큼, 실동작은 `integration` 태그(Testcontainers)로 별도 보증해야 한다.
- Redis 장애를 앱 장애로 전파하지 않기 위해 커맨드 타임아웃을 짧게(2s) — 정합성보다 가용성 우선.

## 남은 이슈 / 후속 작업
- 후속 고려사항(Redis 이중화, 세션-리프레시 정책 등)은 ADR-0006에 정리. 이 문서에 별도 추가 없음.

## 링크
- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/204
- ADR: [docs/adr/0006-redis-for-auth-ttl-data.md](../../docs/adr/0006-redis-for-auth-ttl-data.md)
