# Postman 시나리오 — 로그아웃 (POST /api/auth/logout)

> 모든 응답 예시는 실제 MySQL(`docker compose up -d` → `dongne-mysql`)에 연결된 서버(`./gradlew bootRun`, `SERVER_PORT=18090`)에 curl로 직접 요청해 확인한 값이다(2026-07-03).

## 현재 검증 상태
- [x] 로그인한 사용자가 로그아웃하면 200 확인 완료
- [x] 로그아웃을 두 번 연속 호출해도 둘 다 200(멱등) 확인 완료
- [x] 로그아웃 이후 기존 Refresh Token으로 재발급 시도 시 401 `REFRESH_TOKEN_NOT_FOUND` 확인 완료
- [x] 인증 헤더 없이 로그아웃 시도 시 401 `UNAUTHORIZED` 확인 완료
- [x] 로그아웃 이후에도 기존 Access Token으로 다른 보호 API(`GET /api/members/me`) 호출은 계속 성공함을 확인 완료 (Stateless 정책 — 아래 "동작 방식" 참고)
- [x] `refresh_tokens` 테이블에서 해당 회원의 row가 실제로 삭제됨을 DB 직접 조회로 확인 완료

---

## 프론트 연동 가이드 (요약)

| 항목 | 내용 |
|---|---|
| 요청 방식 | `POST /api/auth/logout`, 요청 본문 없음 |
| 필요 헤더 | `Authorization: Bearer {accessToken}` (필수 — 없으면 401) |
| 성공 응답 | `200`, `data` 없음 |
| 실패 응답 | 토큰 없음/무효 → `401 UNAUTHORIZED` (기존 공통 인증 실패 처리, 로그아웃 전용 에러 아님) |
| 쿠키 사용 여부 | **사용하지 않음.** Access/Refresh Token 모두 로그인·재발급 응답의 JSON `data` 필드로만 내려간다. 서버가 `Set-Cookie`로 아무것도 지우지 않으므로, 프론트가 직접 클라이언트 저장소(메모리/localStorage 등)에서 토큰을 지워야 한다 |
| 멱등성 | 여러 번 호출해도 항상 200. 이미 로그아웃된 상태에서 다시 호출해도 에러가 나지 않는다 |

**로그아웃 후 프론트 처리 흐름**
1. `POST /api/auth/logout` 호출 (Access Token 헤더 포함)
2. 응답 상태와 무관하게(멱등이므로 실패할 일이 거의 없지만, 네트워크 오류 등으로 실패해도) 클라이언트에 저장된 `accessToken`/`refreshToken`을 즉시 삭제
3. 로그인 페이지 등으로 이동

**⚠️ 반드시 알아야 할 동작 방식 — Access Token은 즉시 무효화되지 않는다**

이 API는 서버에 저장된 **Refresh Token만** 삭제한다. JWT는 Stateless로 검증되므로, 로그아웃 시점에 사용자가 들고 있던 Access Token 자체는 서버가 즉시 폐기할 방법이 없다 — 그 Access Token은 자신의 만료 시각(최대 15분)까지 계속 유효하며, 그 사이엔 다른 보호 API 호출이 여전히 성공한다(아래 시나리오 6번 참고). 로그아웃으로 실제 막히는 것은 **"그 세션으로 새 Access Token을 재발급받는 것"**이다. 프론트는 이 점을 감안해 로그아웃 시 반드시 2번(클라이언트 저장소에서 토큰 삭제)까지 수행해야 하며, "서버가 알아서 즉시 차단해줄 것"이라고 가정하면 안 된다.

---

## 1) 성공 — 로그인한 사용자가 로그아웃

**Request**
```
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

## 2) 성공 — 로그아웃을 두 번 연속 호출해도 둘 다 200 (멱등)

같은 Access Token으로 `/api/auth/logout`을 연속으로 두 번 호출.

**Response(1차)** `200 OK`, **Response(2차)** `200 OK` — 응답 동일. 두 번째 호출 시점엔 이미 삭제된 상태라 서버 내부적으로는 "삭제할 row 없음"이지만, 별도 존재 확인 없이 삭제 쿼리를 그대로 실행하므로 에러 없이 통과한다.

## 3) 실패 — 인증 헤더 없이 로그아웃 시도

**Request**
```
POST /api/auth/logout
(Authorization 헤더 없음)
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-07-03T15:35:27.6993089"
}
```

## 4) 로그아웃 이후 기존 Refresh Token으로 재발급 시도 → 실패

로그아웃으로 삭제된 Refresh Token을 그대로 `/api/auth/reissue`에 사용.

**Request**
```
POST /api/auth/reissue
Content-Type: application/json
```
```json
{ "refreshToken": "eyJhbGciOiJIUzUxMiJ9...(로그아웃 전 발급된 값)" }
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "REFRESH_TOKEN_NOT_FOUND",
  "message": "Refresh Token 정보를 찾을 수 없습니다. 다시 로그인해주세요.",
  "timestamp": "2026-07-03T15:35:27.5480177"
}
```
(신규 ErrorCode 아님 — `docs/postman/auth-refresh-token.md`에 정의된 기존 `REFRESH_TOKEN_NOT_FOUND`(AUTH_007)를 그대로 재사용한다.)

## 5) 로그아웃 이후에도 기존 Access Token으로 다른 보호 API는 계속 동작 (Stateless 정책 실측)

**Request**
```
GET /api/members/me
Authorization: Bearer {logout 전에 발급받은 accessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "memberId": 3,
    "email": "logout-demo@example.com",
    "nickname": "logoutDemo",
    "role": "ROLE_USER",
    "status": "ACTIVE",
    "createdAt": "2026-07-03T15:35:05.577932"
  }
}
```
위 "동작 방식" 절에서 설명한 대로, 로그아웃은 Access Token 자체를 무효화하지 않으므로 이 응답은 의도된 동작이다.

## 6) DB 확인 — Refresh Token row 삭제

```
mysql> SELECT id, member_id FROM refresh_tokens;
```
로그아웃 전에는 해당 회원의 row가 있었으나, 로그아웃 호출 이후 조회하면 그 회원의 row만 사라지고 다른 회원의 row는 그대로 남아있음을 확인했다(회원별로 독립적으로 삭제됨).

---

## 검증 체크리스트

- [x] 로그인한 사용자가 로그아웃하면 200 반환
- [x] 로그아웃을 여러 번 호출해도 항상 200(멱등)
- [x] 인증 헤더 없이 로그아웃 시도 시 401 `UNAUTHORIZED`
- [x] 로그아웃 후 기존 Refresh Token으로 재발급 시도 시 401 `REFRESH_TOKEN_NOT_FOUND`
- [x] 로그아웃 후에도 기존 Access Token은 자연 만료 전까지 다른 보호 API에 그대로 사용 가능(Stateless 정책, 의도된 동작)
- [x] DB에서 해당 회원의 `refresh_tokens` row만 정확히 삭제되고 다른 회원 row는 영향받지 않음
