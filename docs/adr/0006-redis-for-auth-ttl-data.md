# 0006. 인증 관련 TTL 데이터를 Redis로 관리

> 상태: Accepted · 날짜: 2026-07-08

## 배경 (Context)

Auth 도메인에는 태생적으로 유효기간(TTL)이 있는 데이터가 여러 개 있었다: Refresh Token, 이메일 인증 코드, 비밀번호 재설정 토큰. 지금까지는 모두 MySQL 테이블(JPA 엔티티)에 `expires_at` 컬럼을 두고 애플리케이션 코드가 직접 만료를 판정했다. 여기에 더해 로그인 실패 횟수 제한(brute-force 방지)도 새로 필요했는데, 이 역시 "N분 동안만 유효한 카운터"라 같은 성격의 데이터다.

이런 데이터를 계속 RDBMS에 두면:
- 만료 판정을 코드가 직접 하고(`isExpired(now)`), 만료된 row를 지우는 배치/정리 작업이 없어 테이블이 계속 쌓인다.
- MySQL은 이 데이터의 특성(초 단위 TTL, 단순 key-value, 읽기/쓰기 위주)에 비해 무겁다.

## 결정 (Decision)

**TTL이 본질인 인증 데이터는 Redis에 저장**하고, TTL 자체를 Redis의 `EXPIRE`에 위임한다(애플리케이션이 만료를 직접 계산·정리하지 않는다).

| 데이터 | Key | TTL | 비고 |
| --- | --- | --- | --- |
| Refresh Token | `auth:refresh:{memberId}` | Refresh Token 만료시간과 동일(기본 7일) | 값=토큰 문자열. `SET`이 upsert라 insert/update 분기 불필요 |
| 로그인 실패 횟수 | `auth:login:fail:{email}` | 10분(최초 실패 시점부터 고정 윈도우) | `INCR` + 최초 증가 시에만 `EXPIRE`. 5회 도달 시 이후 로그인 즉시 차단 |
| 이메일 인증 코드 | `auth:email:verify:{email}` | 5분 | 값=6자리 코드. 재요청 시 덮어쓰기 |
| 비밀번호 재설정 토큰 | `auth:password:reset:member:{memberId}` → tokenHash<br>`auth:password:reset:token:{tokenHash}` → memberId | 30분 | 재설정 링크에 email이 없어(token만 있음) 양방향 2-key가 필요 |

프로파일별 구현 분리(dev/prod는 Redis, test는 인메모리)로 `./gradlew test`는 외부 Redis 없이 계속 통과한다.

**"인증 완료" 여부는 TTL 데이터가 아니므로 DB에 남긴다.** 이메일 인증은 코드 자체(5분)만 Redis에 두고, `email_verifications` 테이블에는 "인증 완료" 사실만 남긴다 — 인증 후 코드 TTL이 지나도 회원가입은 계속 통과해야 하기 때문이다(코드 TTL과 인증 완료 상태의 TTL은 서로 다른 개념).

**Refresh Token Rotation**도 이번에 함께 적용했다: `/reissue` 성공 시마다 새 Refresh Token을 발급·저장하고 쿠키도 다시 내려보낸다. 탈취된 옛 토큰이 재사용되면 이미 교체된 뒤라 저장값과 불일치해 실패하므로, 재사용 탐지 효과가 있다.

**Redis 커맨드 타임아웃(`spring.data.redis.timeout`)을 2초로 명시**했다. 기본값(Lettuce 60초) 그대로 두면 Redis 장애 시 로그인·재발급·로그아웃 요청 하나하나가 최대 1분씩 붙잡혀 톰캣 스레드풀을 고갈시킬 수 있다(수동 테스트로 실제 확인).

**DB 마이그레이션**: `email_verifications`의 `code`/`sent_at`/`expires_at` 컬럼과 `password_reset_tokens` 테이블은 더 이상 쓰이지 않는다. Flyway([ADR 0005](0005-flyway-migration.md))가 이미 도입돼 있으므로, 수동 스크립트 대신 `backend/src/main/resources/db/migration/V2__drop_unused_email_verification_and_password_reset_columns.sql`로 정리한다 — prod는 배포 시 Flyway가 자동 적용하고, dev는 Flyway가 꺼져 있어([ADR 0005](0005-flyway-migration.md)) 별도 안내가 필요하다. 절차는 [runbook/db-migrations.md](../runbook/db-migrations.md) 참고.

## 결과 (Consequences)

**좋은 점**
- 만료 정리가 필요 없다(TTL 자연 만료). 배치/스케줄러 불필요.
- 로그인 실패 카운터처럼 RDBMS로 만들면 어색한 데이터도 자연스럽게 표현된다.
- 장애 시 응답이 예측 가능한 시간(수 초) 안에 끝난다(타임아웃 명시).

**감수하는 비용 / 후속**
- **fail-closed 정책**: 로그인·재발급은 Redis 장애 시 요청 자체가 실패한다(예외 전파). 로그아웃만 fail-open(삭제 실패해도 200, TTL로 자연 정리). 즉 Redis 장애는 곧 "로그인 불가"로 이어진다 — 가용성이 Redis에 종속된다. 향후 Redis를 이중화(Sentinel/Cluster)하거나, 최소한 알림·모니터링을 붙이는 후속이 필요하다.
- Redis TTL로 자연 만료된 코드/토큰은 "존재한 적 없음"과 구분이 안 된다. 그래서 `EXPIRED_VERIFICATION_CODE`/`EXPIRED_RESET_TOKEN` 에러코드는 더 이상 발생하지 않고(코드에 선언만 남아있음), 두 경우 모두 NOT_FOUND/INVALID 계열로 응답한다. 사용자에게는 "다시 요청하라"는 결론이 같아 실질적 영향은 적다고 판단했다.
- 이번 범위에서 제외한 것: Access Token을 Redis에 두는 것(블랙리스트/화이트리스트), 일반 캐싱, 분산 락. 필요해지면 별도 ADR로 논의한다.

## 대안 (Alternatives)

- **계속 MySQL + 배치 정리**: 추가 인프라(Redis) 없이 갈 수 있지만, 만료 정리 배치를 새로 만들어야 하고 로그인 실패 카운터처럼 고빈도 증가·조회가 필요한 데이터에는 부적합(매 로그인 시도마다 트랜잭션 오버헤드).
- **Access Token까지 Redis로(완전 세션화)**: 즉시 무효화 등 이점이 있으나, 모든 인증 요청이 Redis에 종속되어 이번 범위(Refresh Token/로그인 제한/인증 코드/재설정 토큰)보다 훨씬 큰 변경이라 보류.
