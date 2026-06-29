# Postman 시나리오 — 관리자 회원 관리 (GET·PATCH /api/admin/members)

> 아래는 **예상 응답**이다. 실제 MySQL 서버(`docker compose up -d` → `./gradlew bootRun`) 기동 후 Postman으로 각 케이스를 요청해 확인하고, `timestamp`·`memberId` 등은 실제 값으로 갱신한 뒤 맨 아래 체크리스트를 체크한다.
> 관리자 계정 시드(PR 1)가 적용되어, **시드 계정으로 로그인**하면 ROLE_ADMIN 토큰을 받는다(아래 사전 조건).

## 공통
- Method: `GET`
- URL: `/api/admin/members`, `/api/admin/members/{memberId}`
- 인증: **ROLE_ADMIN 필요** (`Authorization: Bearer {adminAccessToken}`)
  - 미인증(토큰 없음/오류) → 401 `UNAUTHORIZED`
  - 인증됐으나 일반 사용자(ROLE_USER) → 403 `FORBIDDEN`
- 목록은 **상태(ACTIVE/SUSPENDED/DELETED) 무관 전체 회원**을 반환한다.
- 정렬은 별도 지정하지 않았다(기본 `id` 순). 정렬 요구가 생기면 후속 작업으로 추가한다.

### 사전 조건 — 관리자 토큰 발급
서버 기동 시 `AdminAccountInitializer` 가 시드한 관리자 계정으로 로그인한다.
```
POST /api/auth/login
{ "email": "admin@dongnemarket.com", "password": "admin1234!" }
→ data.accessToken (ROLE_ADMIN 토큰)
```
> 시드 계정이 없는 환경이면, 일반 가입 후 `UPDATE members SET role='ROLE_ADMIN' WHERE email=...` 로 승격한 뒤 로그인해도 된다(승격 후 로그인 순서 필수).

---

# GET /api/admin/members — 회원 목록 조회

## 1) 성공 — 관리자 토큰 (상태 무관 전체)

**Request**
```
GET /api/admin/members
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "memberId": 1,
      "email": "admin@example.com",
      "nickname": "adminUser",
      "role": "ROLE_ADMIN",
      "status": "ACTIVE",
      "createdAt": "2026-06-28T10:00:00",
      "deletedAt": null
    },
    {
      "memberId": 2,
      "email": "user1@example.com",
      "nickname": "user1",
      "role": "ROLE_USER",
      "status": "ACTIVE",
      "createdAt": "2026-06-28T10:01:00",
      "deletedAt": null
    },
    {
      "memberId": 3,
      "email": "left@example.com",
      "nickname": "leftUser",
      "role": "ROLE_USER",
      "status": "DELETED",
      "createdAt": "2026-06-28T10:02:00",
      "deletedAt": "2026-06-28T11:00:00"
    }
  ]
}
```
> DELETED 회원(`memberId: 3`)도 포함된다 — 일반 목록과 달리 관리자는 전체를 본다.

---

## 2) 실패 — 일반 사용자(ROLE_USER) 접근

ROLE_USER 토큰으로 관리자 경로 호출.

**Request**
```
GET /api/admin/members
Authorization: Bearer {userAccessToken}
```

**Response** `403 Forbidden`
```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "접근 권한이 없습니다.",
  "timestamp": "2026-06-28T10:05:00.000000"
}
```

---

## 3) 실패 — 토큰 없음

**Request**
```
GET /api/admin/members
(Authorization 헤더 없음)
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-28T10:06:00.000000"
}
```

---

# GET /api/admin/members/{memberId} — 회원 상세 조회

## 1) 성공 — 관리자 토큰

**Request**
```
GET /api/admin/members/2
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "memberId": 2,
    "email": "user1@example.com",
    "nickname": "user1",
    "role": "ROLE_USER",
    "status": "ACTIVE",
    "createdAt": "2026-06-28T10:01:00",
    "deletedAt": null
  }
}
```

---

## 2) 실패 — 존재하지 않는 회원

**Request**
```
GET /api/admin/members/999999
Authorization: Bearer {adminAccessToken}
```

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "MEMBER_NOT_FOUND",
  "message": "회원을 찾을 수 없습니다.",
  "timestamp": "2026-06-28T10:07:00.000000"
}
```

---

## 3) 실패 — 권한/인증 (목록과 동일)
- 일반 사용자 토큰 → `403 FORBIDDEN`
- 토큰 없음 → `401 UNAUTHORIZED`

---

# PATCH /api/admin/members/{memberId}/status — 회원 상태 변경

> 관리자가 회원 상태를 `ACTIVE`/`SUSPENDED`/`DELETED` 로 변경한다. `DELETED` 로 바꾸면 `deletedAt` 이 기록되고, `ACTIVE`/`SUSPENDED` 로 바꾸면 초기화된다.

## 1) 성공 — 관리자

**Request**
```
PATCH /api/admin/members/2/status
Authorization: Bearer {adminAccessToken}
Content-Type: application/json
```
```json
{ "status": "SUSPENDED" }
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "memberId": 2,
    "email": "user1@example.com",
    "nickname": "user1",
    "role": "ROLE_USER",
    "status": "SUSPENDED",
    "createdAt": "2026-06-28T10:01:00",
    "deletedAt": null
  }
}
```

## 2) 실패 — 잘못된 상태 값

**Request Body**
```json
{ "status": "INVALID" }
```
**Response** `400 Bad Request`
```json
{ "status": 400, "error": "INVALID_MEMBER_STATUS", "message": "잘못된 회원 상태 값입니다.", "timestamp": "2026-06-28T10:08:00.000000" }
```

## 3) 실패 — 그 외
- 존재하지 않는 회원 → `404 MEMBER_NOT_FOUND`
- 일반 사용자 → `403 FORBIDDEN` / 토큰 없음 → `401 UNAUTHORIZED`

---

## 검증 체크리스트

### GET /api/admin/members
- [ ] 관리자 성공 시 200 + `data` 배열(상태 무관 전체, DELETED 포함)
- [ ] 각 원소에 memberId/email/nickname/role/status/createdAt/deletedAt 포함
- [ ] 일반 사용자 → 403 + `FORBIDDEN`(COMMON_004)
- [ ] 토큰 없음 → 401 + `UNAUTHORIZED`(COMMON_003)

### GET /api/admin/members/{memberId}
- [ ] 관리자 성공 시 200 + 해당 회원 정보
- [ ] 없는 회원 → 404 + `MEMBER_NOT_FOUND`(MEMBER_001)
- [ ] 일반 사용자 → 403 + `FORBIDDEN`(COMMON_004)
- [ ] 토큰 없음 → 401 + `UNAUTHORIZED`(COMMON_003)

### PATCH /api/admin/members/{memberId}/status
- [ ] 관리자 성공 200 + 변경된 status 반환 (DELETED 시 deletedAt 기록, 재활성 시 초기화)
- [ ] 잘못된 상태 값 → 400 + `INVALID_MEMBER_STATUS`(ADMIN_002)
- [ ] 없는 회원 → 404 + `MEMBER_NOT_FOUND`(MEMBER_001)
- [ ] 일반 사용자 → 403 `FORBIDDEN` / 토큰 없음 → 401 `UNAUTHORIZED`
