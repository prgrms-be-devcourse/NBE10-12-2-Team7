# Postman 시나리오 — 내 정보 관리 (GET · PATCH · DELETE /api/members/me)

> 모든 응답 예시는 실제 MySQL(`docker compose up -d` → `dongne-mysql`)에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 요청해 확인한 값이다(2026-06-26).

## 현재 검증 상태
- [x] MySQL Docker 연결 및 `members` 테이블 확인 완료
- [x] `GET /api/members/me` 3가지 케이스 실제 호출로 확인 완료
- [x] `PATCH /api/members/me` 4가지 케이스 실제 호출로 확인 완료
- [x] `DELETE /api/members/me` 3가지 케이스 실제 호출로 확인 완료

## 공통
- Headers: `Authorization: Bearer {accessToken}` (로그인 후 발급된 토큰)
- 모든 엔드포인트 인증 필요 (미인증 시 401)

### 토큰 발급 방법 (사전 조건)
```
POST /api/auth/signup → POST /api/auth/login → accessToken 복사
```

---

# GET /api/members/me — 내 정보 조회

## 1) 성공

**Request**
```
GET /api/members/me
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "memberId": 3,
    "email": "login_test@example.com",
    "nickname": "loginUser",
    "role": "ROLE_USER",
    "status": "ACTIVE",
    "createdAt": "2026-06-26T15:08:03.17954"
  }
}
```

---

## 2) 실패 — 토큰 없음

**Request**
```
GET /api/members/me
(Authorization 헤더 없음)
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-26T15:11:19.7022158"
}
```

---

## 3) 실패 — 유효하지 않은 토큰

**Request**
```
GET /api/members/me
Authorization: Bearer invalid.jwt.token
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-26T15:11:19.8052463"
}
```

> 참고: `INVALID_TOKEN`(AUTH_004)이 정의되어 있으나, 잘못된 토큰도 `UNAUTHORIZED`(COMMON_003)로 반환된다. 구분이 필요하면 `global/security/jwt/` 수정 필요 (팀장 영역).

---

# PATCH /api/members/me — 내 정보 수정

- 수정 가능 항목: `nickname`만
- `email`, `role`, `status`는 수정 불가

## 1) 성공

**Request**
```
PATCH /api/members/me
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "nickname": "updatedNick"
}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "memberId": 3,
    "email": "login_test@example.com",
    "nickname": "updatedNick",
    "role": "ROLE_USER",
    "status": "ACTIVE",
    "createdAt": "2026-06-26T15:08:03.17954"
  }
}
```

---

## 2) 실패 — 토큰 없음

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-26T15:11:32.4327897"
}
```

---

## 3) 실패 — 닉네임 중복

다른 회원이 이미 사용 중인 닉네임으로 수정 시도.

**Request Body**
```json
{
  "nickname": "takenNick"
}
```

**Response** `409 Conflict`
```json
{
  "status": 409,
  "error": "DUPLICATE_NICKNAME",
  "message": "이미 사용 중인 닉네임입니다.",
  "timestamp": "2026-06-26T15:11:32.5548671"
}
```

---

## 4) 실패 — Validation 오류 (닉네임 1자)

닉네임은 2자 이상 20자 이하.

**Request Body**
```json
{
  "nickname": "x"
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_INPUT_VALUE",
  "message": "닉네임은 2자 이상 20자 이하로 입력해주세요.",
  "timestamp": "2026-06-26T15:11:32.6681202"
}
```

---

# DELETE /api/members/me — 회원 탈퇴

- 소프트 삭제: `status = DELETED`, `deleted_at` 기록
- 탈퇴 후 해당 계정으로 로그인 불가 (400 DELETED_MEMBER)
- 기존 JWT는 만료 전까지 유효하나 재로그인 불가

## 1) 성공

**Request**
```
DELETE /api/members/me
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

---

## 2) 탈퇴 후 로그인 시도 (정상 차단 확인)

탈퇴 직후 같은 계정으로 로그인 시도.

**Request**
```
POST /api/auth/login
Content-Type: application/json
```
```json
{
  "email": "delete2@example.com",
  "password": "password123"
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "DELETED_MEMBER",
  "message": "탈퇴한 회원입니다.",
  "timestamp": "2026-06-26T15:11:42.2056834"
}
```

---

## 3) 실패 — 토큰 없음

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-26T15:11:42.3240767"
}
```

---

## 검증 체크리스트

### GET /api/members/me
- [x] 성공 시 200 + id/email/nickname/role/status/createdAt 포함
- [x] 토큰 없음 → 401 + `UNAUTHORIZED`
- [x] 잘못된 토큰 → 401 + `UNAUTHORIZED`

### PATCH /api/members/me
- [x] 성공 시 200 + 변경된 nickname 확인
- [x] 토큰 없음 → 401 + `UNAUTHORIZED`
- [x] 다른 회원 닉네임 → 409 + `DUPLICATE_NICKNAME`(AUTH_002)
- [x] 1자 닉네임 → 400 + `INVALID_INPUT_VALUE`(COMMON_002)

### DELETE /api/members/me
- [x] 성공 시 200 (data 없음)
- [x] 탈퇴 후 로그인 → 400 + `DELETED_MEMBER`(MEMBER_002)
- [x] 토큰 없음 → 401 + `UNAUTHORIZED`
