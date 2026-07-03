# Postman 시나리오 — Refresh Token (로그인 발급 · POST /api/auth/reissue)

> 모든 응답 예시는 실제 로컬 서버(`./gradlew bootRun`, 8080)에 curl로 직접 요청해 확인한 값이다(2026-07-03, Refresh Token Cookie 전환 반영).

## 현재 검증 상태
- [x] 회원가입 → 로그인 시 accessToken은 JSON 응답으로, refreshToken은 `Set-Cookie`(HttpOnly)로 발급 확인 완료
- [x] `POST /api/auth/reissue`가 `Authorization` 헤더 없이(쿠키만으로) 호출 가능함을 확인 완료 (SecurityConfig permitAll)
- [x] 재발급 시 새 accessToken 발급, refreshToken 쿠키는 재설정하지 않음(회전 없음) 확인 완료
- [x] 잘못된 Refresh Token 쿠키 케이스(쿠키 없음/형식 오류/위조/DB 미존재/저장값 불일치) 실제 호출로 확인 완료
- [x] `refresh_tokens` 테이블 — 회원당 1개만 유지(재로그인 시 같은 row가 교체됨), `member_id` UNIQUE 인덱스 확인 완료
- [x] 만료된 Refresh Token 케이스는 실제 서버 호출로는 재현하기 어려워(짧은 TTL로 서버를 재기동해야 함) 테스트 코드로 검증 — 아래 "만료 케이스" 절 참고

## 공통
- Access Token 만료: 15분(900초), Refresh Token 만료: 7일(604800초)
- Refresh Token은 회원당 1개만 DB에 저장되며, 재로그인 시 기존 값을 교체한다(회전 없음 — 재발급 시에도 그대로 유지)
- Access Token과 Refresh Token은 JWT `type` 클레임(`access`/`refresh`)으로 구분되며, 서로 용도를 바꿔 쓸 수 없다(코드 리뷰 후 보강 — 아래 "보안 보강 사항" 참고)
- `reissue()`도 `login()`과 동일하게 SUSPENDED/DELETED 회원을 차단한다(코드 리뷰 후 보강)

## Refresh Token 저장 방식 전환 — HttpOnly Cookie (2026-07-03)

기존에는 로그인/재발급 응답 JSON `data.refreshToken` 필드로 Refresh Token을 내려주고 프론트가 직접 저장/전송했으나, XSS로 탈취될 수 있다는 우려로 **HttpOnly Cookie 기반으로 전환**했다.

- 로그인(`POST /api/auth/login`) 성공 시 서버가 `Set-Cookie: refreshToken=...`를 내려주고, **JSON 응답 바디에는 `accessToken`만** 포함한다.
- `POST /api/auth/reissue`는 더 이상 요청 바디를 받지 않는다 — 브라우저가 자동으로 실어 보내는 `refreshToken` 쿠키에서 값을 읽는다(`@CookieValue`). 프론트는 `credentials: 'include'`로 호출해야 쿠키가 전송된다.
- 쿠키 속성: `HttpOnly`(JS로 `document.cookie` 조회 불가), `SameSite=Lax`, `Path=/`, `Secure`는 `auth.cookie.secure` 프로퍼티 기반(기본 `false` — 현재 운영이 HTTP만 지원하기 때문. HTTPS 적용 후 `AUTH_COOKIE_SECURE=true`로 전환). `Domain`은 지정하지 않아 Host-Only 쿠키로 동작한다.
- **`Max-Age`는 로그인 화면의 "자동 로그인" 체크박스(`LoginRequest.autoLogin`)에 따라 분기된다** — 체크(true)하면 `Max-Age=604800`(7일)의 영속 쿠키, 체크 안 함(false, 기본값)이면 `Max-Age`를 아예 지정하지 않는 세션 쿠키(브라우저 종료 시 삭제)로 발급한다. 아래 "자동 로그인 체크박스" 절 참고.
- Access Token은 여전히 JSON 응답으로 내려가며 프론트는 **메모리에만** 보관한다(localStorage/sessionStorage 미사용) — 새로고침하면 사라지고 `/api/auth/reissue`(쿠키 기반)로 복구한다.
- `AuthService`/`RefreshTokenService`/`JwtTokenProvider`/`SecurityConfig`는 이 전환으로 **전혀 변경되지 않았다** — 쿠키 처리는 `AuthController`에서만 이루어진다.

## 자동 로그인 체크박스 — Refresh Token 쿠키 수명 정책 (2026-07-03)

로그인 화면(`/login`)에 "자동 로그인" 체크박스를 추가했다. `POST /api/auth/login` 요청 바디의 `autoLogin`(boolean, 생략 시 `false`) 값에 따라 `Set-Cookie`의 `Max-Age`/`Expires`만 달라지고, 나머지 속성(`HttpOnly`/`SameSite`/`Path`/`Secure`)은 동일하다.

| `autoLogin` | 쿠키 종류 | Max-Age/Expires | 동작 |
|---|---|---|---|
| `true` | 영속 쿠키 | `Max-Age=604800`(7일) | 브라우저를 껐다 켜도 쿠키가 남아있어 자동 로그인 유지 |
| `false`(기본값) | 세션 쿠키 | 없음 | 브라우저를 종료하면 쿠키가 즉시 삭제되어 로그인 상태가 유지되지 않음 |

- Refresh Token JWT 자체의 만료(7일)는 `autoLogin` 값과 무관하게 항상 동일하다 — 달라지는 건 **브라우저가 쿠키를 얼마나 들고 있는지**뿐이다.
- 로그아웃(`POST /api/auth/logout`)은 `autoLogin` 값과 무관하게 항상 `Max-Age=0`으로 즉시 쿠키를 지운다.
- `reissue`는 쿠키를 재설정하지 않으므로(회전 없음) `autoLogin` 분기와 무관하다.

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

## 2) 로그인 — accessToken은 JSON, refreshToken은 HttpOnly 쿠키로 발급

### 2-1) `autoLogin: true` — 영속 쿠키 (Max-Age 있음)

**Request**
```
POST /api/auth/login
Content-Type: application/json
```
```json
{
  "email": "autologin-doc@example.com",
  "password": "password123",
  "autoLogin": true
}
```

**Response** `200 OK`
```
HTTP/1.1 200
Set-Cookie: refreshToken=eyJhbGciOiJIUzUxMiJ9...; Path=/; Max-Age=604800; Expires=Fri, 10 Jul 2026 11:44:19 GMT; HttpOnly; SameSite=Lax
```

### 2-2) `autoLogin: false`(또는 생략) — 세션 쿠키 (Max-Age 없음)

**Request Body**
```json
{
  "email": "autologin-doc@example.com",
  "password": "password123",
  "autoLogin": false
}
```

**Response** `200 OK`
```
HTTP/1.1 200
Set-Cookie: refreshToken=eyJhbGciOiJIUzUxMiJ9...; Path=/; HttpOnly; SameSite=Lax
```
```json
{
  "status": 200,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI4Iiwicm9sZSI6IlJPTEVfVVNFUiIsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3ODMwNzU3MjcsImV4cCI6MTc4MzA3NjYyN30.-eyFE-iHGIclCUNAbLB5rowLSvL_a03IRkYxJ1p03bTulUQwGk_ztDkyEL_FNo1Xvrr48c1_fHAxuaX2SsN8lA"
  }
}
```
`autoLogin`을 아예 생략해도 동일하게 `Max-Age`/`Expires` 없는 세션 쿠키가 내려감을 확인했다(기본값 `false`).

**확인된 사실**
- 응답 바디는 `autoLogin` 값과 무관하게 항상 동일한 형태다(`accessToken`만 존재, `refreshToken` 필드 없음).
- `HttpOnly`, `SameSite=Lax`, `Path=/`는 `autoLogin` 값과 무관하게 항상 붙는다. `Max-Age`/`Expires`만 `autoLogin`에 따라 있거나(2-1) 없다(2-2).
- 로컬 환경은 HTTP라 `Secure` 속성이 붙지 않는다(`auth.cookie.secure=false` 기본값 — HTTPS 적용 후 `AUTH_COOKIE_SECURE=true`로 전환 예정).

---

## 3) Access Token 재발급 — POST /api/auth/reissue

### 3-1) 성공 — 쿠키만으로 호출 (Authorization 헤더·요청 바디 불필요)

`/api/auth/reissue`는 `SecurityConfig`에서 `permitAll`로 등록되어 있고, 이제 요청 바디 없이 브라우저가 자동으로 실어 보내는 `refreshToken` 쿠키만으로 동작한다.

**Request**
```
POST /api/auth/reissue
Cookie: refreshToken=eyJhbGciOiJIUzUxMiJ9...(로그인 응답의 Set-Cookie 값)
(Authorization 헤더 없음, 요청 바디 없음)
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI4Iiwicm9sZSI6IlJPTEVfVVNFUiIsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3ODMwNzU3MjgsImV4cCI6MTc4MzA3NjYyOH0.65QzJHkidY06gPK5Kq_dCd7D9UtebG1jrbzLBlQ60lUb1zf6vs2IDZPT5WaaZkTajZWRt5Wfu0zh2cIt0qhhZQ"
  }
}
```

**확인된 사실**
- 응답에 새 `accessToken`만 있고(로그인 때와 다른 값, `iat` 갱신됨), `refreshToken` 필드는 없다.
- 응답에 `Set-Cookie`가 다시 내려오지 않는다 — Refresh Token은 회전(rotation)되지 않고 기존 쿠키가 그대로 유지된다(확정 범위 8번).

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

### 4-0) 실패 — 쿠키 자체가 없음 (신규 케이스, 쿠키 전환으로 추가됨)

**Request**
```
POST /api/auth/reissue
(Cookie 헤더 없음)
```

**Response** `401 Unauthorized`
```json
{"status":401,"error":"INVALID_REFRESH_TOKEN","message":"유효하지 않은 Refresh Token입니다.","timestamp":"2026-07-03T19:48:48.2474591"}
```

### 4-1) 실패 — 형식 오류 (malformed)

**Request**
```
POST /api/auth/reissue
Cookie: refreshToken=not.a.valid.token
```

**Response** `401 Unauthorized`
```json
{"status":401,"error":"INVALID_REFRESH_TOKEN","message":"유효하지 않은 Refresh Token입니다.","timestamp":"2026-07-03T19:48:48.3764842"}
```

### 4-2) 실패 — 위조(서명 변조) 토큰

정상 발급된 refreshToken 쿠키 값의 `header.payload`는 그대로 두고 서명(3번째 세그먼트)만 임의 문자열로 변조해 쿠키로 전송.

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

---

## 5) 만료된 Refresh Token 케이스 (테스트 코드 기준 검증)

만료 케이스는 실제 서버가 7일짜리 토큰을 즉시 만료시킬 수 없어(서버 재기동 없이는 TTL을 바꿀 수 없음) 실제 호출로 재현하지 않았다. 대신 아래 단위 테스트에서 `refresh-token-validity-seconds=0`으로 구성한 `JwtTokenProvider`로 즉시 만료된 토큰을 생성해 검증했다.

- `RefreshTokenServiceTest#validateAndGetMemberId_expired_throwsExpiredRefreshToken` — 만료된 토큰으로 `validateAndGetMemberId()` 호출 시 `EXPIRED_REFRESH_TOKEN` 발생 확인
- `AuthServiceTest#reissue_expiredRefreshToken_throwsException` — `AuthService.reissue()` 경유로도 동일하게 `EXPIRED_REFRESH_TOKEN` 발생 확인

두 테스트 모두 `./gradlew test` 실행 시 통과함(전체 결과는 아래 요약 참고).

---

## 검증 체크리스트

- [x] 회원가입 성공 시 201 + 회원 정보 반환
- [x] 로그인 성공 시 200 + `accessToken`(15분, JSON 바디) + `refreshToken`(7일, HttpOnly `Set-Cookie`) 발급
- [x] 로그인/재발급 응답 바디에 `refreshToken` 필드가 더 이상 존재하지 않음
- [x] `POST /api/auth/reissue`는 `Authorization` 헤더·요청 바디 없이 쿠키만으로 호출 가능(permitAll)
- [x] 재발급 성공 시 200 + 새 `accessToken`, `Set-Cookie`는 다시 내려오지 않음(회전 없음)
- [x] `refresh_tokens` 테이블은 회원당 1개 row만 유지(재로그인 시 교체, UNIQUE(member_id))
- [x] 쿠키 자체가 없는 요청 → 401 + `INVALID_REFRESH_TOKEN`(AUTH_006, 쿠키 전환으로 추가된 케이스)
- [x] 형식이 깨진 토큰 쿠키 → 401 + `INVALID_REFRESH_TOKEN`(AUTH_006)
- [x] 위조(서명 변조) 토큰 쿠키 → 401 + `INVALID_REFRESH_TOKEN`(AUTH_006)
- [x] DB에 저장된 row가 없는 토큰 → 401 + `REFRESH_TOKEN_NOT_FOUND`(AUTH_007)
- [x] DB 저장값과 다른(교체되어 폐기된) 토큰 → 401 + `INVALID_REFRESH_TOKEN`(AUTH_006)
- [x] 만료된 토큰 → `EXPIRED_REFRESH_TOKEN`(AUTH_005) — 단위 테스트로 검증(실서버 재현 대신)
- [x] 쿠키 속성: `HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure`는 `auth.cookie.secure`(기본 false) 기반, `Domain` 미지정(Host-Only)
- [x] `autoLogin: true` → `Max-Age=604800`(7일)의 영속 쿠키로 발급 (`AuthControllerTest#login_autoLoginTrue_setsPersistentCookieWithMaxAge`)
- [x] `autoLogin: false`(또는 생략) → `Max-Age` 없는 세션 쿠키로 발급 (`AuthControllerTest#login_autoLoginFalse_setsSessionCookieWithoutMaxAge`)
