# Postman 시나리오 — Refresh Token (로그인 발급 · POST /api/auth/reissue)

> 모든 응답 예시는 실제 MySQL(`docker compose up -d` → `dongne-mysql`)에 연결된 서버(`./gradlew bootRun`, `SERVER_PORT=18090`)에 curl로 직접 요청해 확인한 값이다(2026-07-03).

## 현재 검증 상태
- [x] 회원가입 → 로그인 시 accessToken/refreshToken 함께 발급 확인 완료
- [x] `POST /api/auth/reissue`가 `Authorization` 헤더 없이 호출 가능함을 확인 완료 (SecurityConfig permitAll)
- [x] 재발급 시 새 accessToken 발급 + refreshToken은 기존 값 그대로 유지(회전 없음) 확인 완료
- [x] 잘못된 Refresh Token 4가지 케이스(형식 오류/위조/DB 미존재/저장값 불일치) 실제 호출로 확인 완료
- [x] `refresh_tokens` 테이블 — 회원당 1개만 유지(재로그인 시 같은 row가 교체됨), `member_id` UNIQUE 인덱스 확인 완료
- [x] 만료된 Refresh Token 케이스는 실제 서버 호출로는 재현하기 어려워(짧은 TTL로 서버를 재기동해야 함) 테스트 코드로 검증 — 아래 "만료 케이스" 절 참고

## 공통
- Access Token 만료: 15분(900초), Refresh Token 만료: 7일(604800초)
- Refresh Token은 회원당 1개만 DB에 저장되며, 재로그인 시 기존 값을 교체한다(회전 없음 — 재발급 시에도 그대로 유지)
- Access Token과 Refresh Token은 JWT `type` 클레임(`access`/`refresh`)으로 구분되며, 서로 용도를 바꿔 쓸 수 없다(코드 리뷰 후 보강 — 아래 "보안 보강 사항" 참고)
- `reissue()`도 `login()`과 동일하게 SUSPENDED/DELETED 회원을 차단한다(코드 리뷰 후 보강)

## 보안 보강 사항 (코드 리뷰 반영, 2026-07-03)

최초 구현 커밋(`ce5e477`) 리뷰에서 발견된 아래 항목을 같은 브랜치에서 수정했다. 실제 curl 재검증 대신 단위/통합 테스트로 검증했다 — 테스트 파일 참고.

1. **Refresh Token으로 보호 API 인증 우회 가능했던 문제**: Refresh Token에는 `role` 클레임이 없다는 점 외에는 Access Token과 구분할 방법이 없어, 탈취된 Refresh Token을 `Authorization: Bearer`로 그대로 사용하면 `/api/members/me` 같은 일반 보호 API 인증에 통과했다. `JwtTokenProvider.createAccessToken`/`createRefreshToken`에 `type=access`/`type=refresh` 클레임을 추가하고, `getAuthentication()`은 `type=access`가 아니면 인증을 거부하도록 수정했다(위반 시 기존 `JwtAuthenticationFilter`의 `INVALID_TOKEN` 처리 경로를 그대로 탄다). 검증: `SecurityPolicyTest#protectedApi_withRefreshToken_returns401InvalidToken`.
2. **`/api/auth/reissue`에 Access Token을 넣어도 통과하던 문제**: `RefreshTokenService.validateAndGetMemberId()`에 `jwtTokenProvider.isRefreshToken()` 체크를 추가해, Refresh Token이 아닌 토큰(Access Token 포함)은 `INVALID_REFRESH_TOKEN`으로 거부한다. 검증: `RefreshTokenServiceTest#validateAndGetMemberId_accessTokenPresented_throwsInvalidRefreshToken`, `AuthServiceTest#reissue_accessTokenPresented_throwsInvalidRefreshToken`.
3. **`reissue()`가 회원 상태를 재검증하지 않던 문제**: `login()`에 이미 있던 DELETED/SUSPENDED 차단 로직을 `AuthService.validateActiveStatus()`로 추출해 `reissue()`에서도 동일하게 적용했다 — 정지/탈퇴된 회원은 기존 Refresh Token으로도 더 이상 Access Token을 재발급받을 수 없다. 검증: `AuthServiceTest#reissue_deletedMember_throwsException`, `#reissue_suspendedMember_throwsException`.

## 운영 배포 참고사항 — `refresh_tokens` 테이블 마이그레이션 필요

이 프로젝트는 Flyway/Liquibase 등 스키마 마이그레이션 도구를 사용하지 않는다. 로컬/테스트 프로파일은 `hibernate.ddl-auto: update`/`create-drop`이라 `RefreshToken` 엔티티만으로 테이블이 자동 생성되지만, **`application-prod.yml`은 `ddl-auto: validate`(운영 스키마 자동 변경 금지)이고 운영 DB에는 아직 `refresh_tokens` 테이블이 없다.** 이 상태로 `SPRING_PROFILES_ACTIVE=prod`로 배포하면 Hibernate 스키마 검증 단계에서 애플리케이션이 기동에 실패한다.

**배포 전 운영 DB에 아래 DDL을 수동으로 먼저 실행해야 한다** (로컬 `dongne-mysql`에서 `SHOW CREATE TABLE refresh_tokens`로 확인한 실제 Hibernate 생성 스키마 그대로):

```sql
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `member_id` bigint NOT NULL,
  `token` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

체크리스트:
- [ ] 운영 DB에 위 DDL 실행 완료 확인 (배포 담당자/팀장 확인 필요)
- [ ] 실행 후 `SPRING_PROFILES_ACTIVE=prod`로 기동 시 Hibernate `validate`가 통과하는지 확인
- [ ] 이후 신규 테이블이 필요한 기능부터는 Flyway/Liquibase 도입을 팀 차원에서 검토 권장(현재는 스키마 변경마다 수동 DDL 필요)

---

## 1) 회원가입

**Request**
```
POST /api/auth/signup
Content-Type: application/json
```
```json
{
  "email": "refresh-demo@example.com",
  "password": "password123",
  "nickname": "refreshDemo"
}
```

**Response** `201 Created`
```json
{
  "status": 201,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "memberId": 11,
    "email": "refresh-demo@example.com",
    "nickname": "refreshDemo"
  }
}
```

---

## 2) 로그인 — accessToken · refreshToken 함께 발급

**Request**
```
POST /api/auth/login
Content-Type: application/json
```
```json
{
  "email": "refresh-demo@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsInJvbGUiOiJST0xFX1VTRVIiLCJpYXQiOjE3ODMwNDQ1OTUsImV4cCI6MTc4MzA0NTQ5NX0.nfPYmJhfYoskGoUPrgEWAAbqlgJo6ytBHPWKFSSBWumDH08Sx6hjnSrZctrO3JR5VMFxaTQVGn_zvYqMweaBBA",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsImlhdCI6MTc4MzA0NDU5NSwiZXhwIjoxNzgzNjQ5Mzk1fQ.Wq8PMFYFHAEH2Op-q15X5u-6QI_Dgu-K6j3ICtgkPjNyCo4onaxZJYIq5XxtmixRmAS1mn7QHH8093VqMfpRhQ"
  }
}
```

`accessToken`의 `iat`/`exp` 클레임을 디코딩하면 `1783045495 - 1783044595 = 900`초(15분), `refreshToken`은 `1783649395 - 1783044595 = 604800`초(7일) — 요구사항대로 발급됨을 실측으로 확인.

---

## 3) Access Token 재발급 — POST /api/auth/reissue

### 3-1) 성공 — 인증 헤더 없이 호출 가능

`/api/auth/reissue`는 `SecurityConfig`에서 `permitAll`로 등록되어 있어, **`Authorization` 헤더 없이도** 호출된다.

**Request**
```
POST /api/auth/reissue
Content-Type: application/json
(Authorization 헤더 없음)
```
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsImlhdCI6MTc4MzA0NDU5NSwiZXhwIjoxNzgzNjQ5Mzk1fQ.Wq8PMFYFHAEH2Op-q15X5u-6QI_Dgu-K6j3ICtgkPjNyCo4onaxZJYIq5XxtmixRmAS1mn7QHH8093VqMfpRhQ"
}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsInJvbGUiOiJST0xFX1VTRVIiLCJpYXQiOjE3ODMwNDQ2MDIsImV4cCI6MTc4MzA0NTUwMn0.CqNmsMOTiTuzieV-mu3R2HQ4LETNA5uqrj_eUvrUF27TMrKifHt6UCIGu6d1zkNZC9p6jn6aibx8koZaXOPXzA",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsImlhdCI6MTc4MzA0NDU5NSwiZXhwIjoxNzgzNjQ5Mzk1fQ.Wq8PMFYFHAEH2Op-q15X5u-6QI_Dgu-K6j3ICtgkPjNyCo4onaxZJYIq5XxtmixRmAS1mn7QHH8093VqMfpRhQ"
  }
}
```

**확인된 사실**
- 응답의 `accessToken`은 로그인 때와 다른 새 토큰(`iat`가 갱신됨: `1783044595` → `1783044602`).
- 응답의 `refreshToken`은 요청으로 보낸 값과 **완전히 동일** — Refresh Token은 회전(rotation)되지 않고 그대로 유지된다(확정 범위 8번).

### 3-2) DB 확인 — 회원당 1개만 유지

```
mysql> SELECT id, member_id, LEFT(token,20) AS token_prefix, expires_at FROM refresh_tokens ORDER BY id;
id  member_id  token_prefix          expires_at
1   11         eyJhbGciOiJIUzUxMiJ9  2026-07-10 11:09:55.000000

mysql> SHOW INDEX FROM refresh_tokens WHERE Key_name != 'PRIMARY';
Table           Non_unique  Key_name                     Column_name
refresh_tokens  0           UK6vmhlugnhc5iqbcdv1i5ebswn  member_id
```

- `member_id`에 `Non_unique = 0`(UNIQUE 인덱스) 확인.
- 아래 4-4) 시나리오에서 같은 계정으로 재로그인해도 `id=1` row가 그대로 재사용(교체)되고 새 row가 추가되지 않음을 확인(회원당 1개 세션 정책 정상 동작).

---

## 4) 잘못된 Refresh Token 요청

### 4-1) 실패 — 형식 오류 (malformed)

**Request Body**
```json
{ "refreshToken": "not.a.valid.token" }
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "INVALID_REFRESH_TOKEN",
  "message": "유효하지 않은 Refresh Token입니다.",
  "timestamp": "2026-07-03T11:10:09.2645991"
}
```

### 4-2) 실패 — 위조(서명 변조) 토큰

정상 발급된 refreshToken의 `header.payload`는 그대로 두고 서명(3번째 세그먼트)만 임의 문자열로 변조.

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "INVALID_REFRESH_TOKEN",
  "message": "유효하지 않은 Refresh Token입니다.",
  "timestamp": "2026-07-03T11:10:21.9530456"
}
```

### 4-3) 실패 — DB에 저장된 row가 없음

신규 회원가입 → 로그인(refreshToken 발급) 후, 해당 회원의 `refresh_tokens` row를 DB에서 직접 삭제한 뒤 **아직 만료되지 않은** 동일 refreshToken으로 재발급 시도(관리자 강제 폐기·로그아웃 이후 재사용 시도를 재현).

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "REFRESH_TOKEN_NOT_FOUND",
  "message": "Refresh Token 정보를 찾을 수 없습니다. 다시 로그인해주세요.",
  "timestamp": "2026-07-03T11:10:51.3871468"
}
```

### 4-4) 실패 — 저장값 불일치 (교체되어 폐기된 구 토큰 재사용)

같은 계정으로 연속 로그인하면 회원당 1개 정책에 따라 `refresh_tokens` row가 새 토큰으로 교체된다. 이때 **첫 번째 로그인에서 받은(이미 폐기된) refreshToken**으로 재발급을 시도.

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "INVALID_REFRESH_TOKEN",
  "message": "유효하지 않은 Refresh Token입니다.",
  "timestamp": "2026-07-03T11:11:03.1446576"
}
```

### 4-5) 실패(참고) — Validation 오류 (refreshToken 빈 값)

**Request Body**
```json
{ "refreshToken": "" }
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_INPUT_VALUE",
  "message": "Refresh Token은 필수입니다.",
  "timestamp": "2026-07-03T11:11:12.2386113"
}
```

---

## 5) 만료된 Refresh Token 케이스 (테스트 코드 기준 검증)

만료 케이스는 실제 서버가 7일짜리 토큰을 즉시 만료시킬 수 없어(서버 재기동 없이는 TTL을 바꿀 수 없음) 실제 호출로 재현하지 않았다. 대신 아래 단위 테스트에서 `refresh-token-validity-seconds=0`으로 구성한 `JwtTokenProvider`로 즉시 만료된 토큰을 생성해 검증했다.

- `RefreshTokenServiceTest#validateAndGetMemberId_expired_throwsExpiredRefreshToken` — 만료된 토큰으로 `validateAndGetMemberId()` 호출 시 `EXPIRED_REFRESH_TOKEN` 발생 확인
- `AuthServiceTest#reissue_expiredRefreshToken_throwsException` — `AuthService.reissue()` 경유로도 동일하게 `EXPIRED_REFRESH_TOKEN` 발생 확인

두 테스트 모두 `./gradlew test` 실행 시 통과함(전체 결과는 아래 요약 참고).

---

## 검증 체크리스트

- [x] 회원가입 성공 시 201 + 회원 정보 반환
- [x] 로그인 성공 시 200 + `accessToken`(15분)·`refreshToken`(7일) 함께 반환
- [x] `POST /api/auth/reissue`는 `Authorization` 헤더 없이 호출 가능(permitAll)
- [x] 재발급 성공 시 200 + 새 `accessToken` + **입력과 동일한** `refreshToken`(회전 없음)
- [x] `refresh_tokens` 테이블은 회원당 1개 row만 유지(재로그인 시 교체, UNIQUE(member_id))
- [x] 형식이 깨진 토큰 → 401 + `INVALID_REFRESH_TOKEN`(AUTH_006)
- [x] 위조(서명 변조) 토큰 → 401 + `INVALID_REFRESH_TOKEN`(AUTH_006)
- [x] DB에 저장된 row가 없는 토큰 → 401 + `REFRESH_TOKEN_NOT_FOUND`(AUTH_007)
- [x] DB 저장값과 다른(교체되어 폐기된) 토큰 → 401 + `INVALID_REFRESH_TOKEN`(AUTH_006)
- [x] refreshToken 빈 값 → 400 + 공통 `INVALID_INPUT_VALUE`
- [x] 만료된 토큰 → `EXPIRED_REFRESH_TOKEN`(AUTH_005) — 단위 테스트로 검증(실서버 재현 대신)
