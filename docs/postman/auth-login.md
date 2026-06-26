# Postman 시나리오 — 로그인 (POST /api/auth/login)

> 1~5번 응답 예시는 실제 MySQL(`docker compose up -d` → `dongne-mysql`)에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 요청해 확인한 값이다(2026-06-26).

## 현재 검증 상태
- [x] MySQL Docker 연결 및 `members` 테이블 확인 완료
- [x] `POST /api/auth/login` 5가지 케이스(성공/없는이메일/비밀번호틀림/validation/탈퇴회원) 실제 호출로 확인 완료
- [x] 성공 시 JWT Access Token 발급 확인 (HS512 서명, sub=memberId, role 클레임 포함)

## 공통
- Method: `POST`
- URL: `/api/auth/login`
- Headers: `Content-Type: application/json`
- 인증 불필요 (SecurityConfig permitAll)

---

## 1) 성공 — 로그인 및 JWT 발급

**Request Body**
```json
{
  "email": "login_test@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIzIiwicm9sZSI6IlJPTEVfVVNFUiIsImlhdCI6MTc4MjQ1NDE3MywiZXhwIjoxNzgyNDU3NzczfQ.DQxd9WGY..."
  }
}
```

---

## 2) 실패 — 존재하지 않는 이메일

**Request Body**
```json
{
  "email": "none@example.com",
  "password": "password123"
}
```

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "MEMBER_NOT_FOUND",
  "message": "회원을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T15:09:33.9389845"
}
```

---

## 3) 실패 — 비밀번호 불일치

**Request Body**
```json
{
  "email": "login_test@example.com",
  "password": "wrongPassword"
}
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "INVALID_PASSWORD",
  "message": "비밀번호가 일치하지 않습니다.",
  "timestamp": "2026-06-26T15:09:34.1310967"
}
```

---

## 4) 실패 — Validation 오류 (이메일 형식 오류)

**Request Body**
```json
{
  "email": "not-an-email",
  "password": "password123"
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_INPUT_VALUE",
  "message": "이메일 형식이 올바르지 않습니다.",
  "timestamp": "2026-06-26T15:09:34.2516381"
}
```

---

## 5) 실패 — 탈퇴한 회원 로그인 시도

사전 조건: 해당 계정으로 `DELETE /api/members/me`를 호출해 탈퇴 처리한 뒤 재로그인 시도.

**Request Body**
```json
{
  "email": "deleted_test@example.com",
  "password": "password123"
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "DELETED_MEMBER",
  "message": "탈퇴한 회원입니다.",
  "timestamp": "2026-06-26T15:11:11.6209841"
}
```

---

## 검증 체크리스트
- [x] 성공 시 200 + `ApiResponse` 포맷 + JWT accessToken 발급
- [x] 존재하지 않는 이메일 → 404 + `MEMBER_NOT_FOUND`(MEMBER_001)
- [x] 비밀번호 불일치 → 401 + `INVALID_PASSWORD`(AUTH_003)
- [x] 이메일 형식 오류 → 400 + `INVALID_INPUT_VALUE`(COMMON_002)
- [x] 탈퇴 회원 → 400 + `DELETED_MEMBER`(MEMBER_002)
