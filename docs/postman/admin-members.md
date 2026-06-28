# Postman 시나리오 — 관리자 회원 조회 (GET /api/admin/members, GET /api/admin/members/{memberId})

> 아래는 **예상 응답**이다. 실제 MySQL 서버(`docker compose up -d` → `./gradlew bootRun`) 기동 후 Postman으로 각 케이스를 요청해 확인하고, `timestamp`·`memberId` 등은 실제 값으로 갱신한 뒤 맨 아래 체크리스트를 체크한다.
> 관리자 계정 시드(PR 1)가 아직 없으므로, 토큰은 **수동 승격**으로 발급한다(아래 사전 조건). PR 1 머지 후에는 시드 관리자 계정으로 바로 로그인하면 된다.

## 공통
- Method: `GET`
- URL: `/api/admin/members`, `/api/admin/members/{memberId}`
- 인증: **ROLE_ADMIN 필요** (`Authorization: Bearer {adminAccessToken}`)
  - 미인증(토큰 없음/오류) → 401 `UNAUTHORIZED`
  - 인증됐으나 일반 사용자(ROLE_USER) → 403 `FORBIDDEN`
- 목록은 **상태(ACTIVE/SUSPENDED/DELETED) 무관 전체 회원**을 반환한다.
- 정렬은 별도 지정하지 않았다(기본 `id` 순). 정렬 요구가 생기면 후속 작업으로 추가한다.

### 사전 조건 — 관리자 토큰 발급 (시드 전까지 수동)
```
1. 회원 가입
   POST /api/auth/signup
   { "email": "admin@example.com", "password": "password123", "nickname": "adminUser" }

2. 관리자 승격 (MySQL에서 직접 — IntelliJ Database 도구 또는 mysql 클라이언트)
   UPDATE members SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';

3. 로그인 → accessToken 복사
   POST /api/auth/login
   { "email": "admin@example.com", "password": "password123" }
   → data.accessToken (이 토큰에 ROLE_ADMIN 이 담긴다)
```
> ⚠ 반드시 **승격(2) 후 로그인(3)**. 권한은 로그인 시점에 토큰에 박히므로, 가입 직후 받은 토큰은 ROLE_USER 다.

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
