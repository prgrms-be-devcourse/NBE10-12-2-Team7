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
