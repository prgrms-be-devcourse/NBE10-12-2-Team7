# Postman 시나리오 — 비밀번호 변경 (PATCH /api/members/me/password)

> 실제 `dongne-mysql`에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 요청해 확인했다(2026-07-06).

## 현재 검증 상태
- [x] 성공 시 200 확인
- [x] 변경된 비밀번호로 재로그인 성공 확인
- [x] 변경 성공 시 기존 Refresh Token 삭제 → 재발급 시도 시 401 `REFRESH_TOKEN_NOT_FOUND` 확인 (다른 세션도 강제 재로그인 유도)
- [x] 현재 비밀번호 불일치 → 401 `INVALID_PASSWORD`
- [x] 새 비밀번호가 현재와 동일 → 400 `SAME_AS_OLD_PASSWORD`
- [x] 새 비밀번호 정책 위반 → 400 `INVALID_INPUT_VALUE`
- [x] 토큰 없이 요청 → 401 `UNAUTHORIZED`
- [x] 탈퇴/정지 회원 상태 확인 → 400 `DELETED_MEMBER` / 403 `SUSPENDED_MEMBER`
- [x] DB에서 비밀번호가 BCrypt 해시로 저장됨을 직접 확인
- [x] 테스트 데이터 정리 및 서버 종료 완료

## 공통
- Method: `PATCH`
- URL: `/api/members/me/password`
- Headers: `Content-Type: application/json`, `Authorization: Bearer {accessToken}`
- 새 비밀번호는 회원가입과 동일한 정책(10~64자, 영문+숫자+특수문자 포함, 공백 불가)을 따른다.
- **변경 성공 시 저장된 Refresh Token이 삭제된다(단일 세션 정책이라 사실상 모든 기기에서 재로그인 필요).**

---

## 1) 성공 — 비밀번호 변경

**Request Body**
```json
{
  "currentPassword": "password123!",
  "newPassword": "newPassword123!"
}
```

**Response** `200 OK` (실제 호출 결과)
```json
{"status":200,"message":"요청이 성공적으로 처리되었습니다."}
```

변경 후 새 비밀번호로 즉시 로그인 성공을 확인했다:
```json
{"status":200,"message":"로그인이 완료되었습니다.","data":{"accessToken":"..."}}
```

---

## 2) 성공(부수효과) — 기존 Refresh Token 무효화

비밀번호 변경 전 로그인해서 받은 Refresh Token 쿠키로 변경 후 재발급을 시도.

**Response** `401 Unauthorized` (실제 호출 결과)
```json
{"status":401,"error":"REFRESH_TOKEN_NOT_FOUND","message":"Refresh Token 정보를 찾을 수 없습니다. 다시 로그인해주세요.","timestamp":"2026-07-06T13:37:22.9091386"}
```

---

## 3) 실패 — 현재 비밀번호 불일치

**Request Body**
```json
{
  "currentPassword": "wrongPassword123!",
  "newPassword": "anotherPassword123!"
}
```

**Response** `401 Unauthorized` (실제 호출 결과)
```json
{"status":401,"error":"INVALID_PASSWORD","message":"비밀번호가 일치하지 않습니다.","timestamp":"2026-07-06T13:37:34.0501271"}
```

---

## 4) 실패 — 새 비밀번호가 현재 비밀번호와 동일

**Request Body**
```json
{
  "currentPassword": "newPassword123!",
  "newPassword": "newPassword123!"
}
```

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"SAME_AS_OLD_PASSWORD","message":"새 비밀번호는 현재 비밀번호와 달라야 합니다.","timestamp":"2026-07-06T13:37:34.341807"}
```

---

## 5) 실패 — 새 비밀번호 정책 위반(특수문자 없음)

**Request Body**
```json
{
  "currentPassword": "newPassword123!",
  "newPassword": "nospecialchar123"
}
```

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"INVALID_INPUT_VALUE","message":"비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 하며 공백을 포함할 수 없습니다.","timestamp":"2026-07-06T13:37:34.495797"}
```

---

## 6) 실패 — 토큰 없이 요청

**Response** `401 Unauthorized` (실제 호출 결과)
```json
{"status":401,"error":"UNAUTHORIZED","message":"인증이 필요합니다.","timestamp":"2026-07-06T13:37:34.6295962"}
```

---

## MySQL 직접 확인

```
mysql> SELECT email, LEFT(password, 10) as password_prefix FROM members WHERE email LIKE 'pw-%';
email                       password_prefix
pw-real-test@example.com    $2a$10$lrp
pw-token-test@example.com   $2a$10$63M
```

비밀번호가 평문이 아닌 BCrypt 해시로 저장됨을 확인했다.

## 검증 체크리스트
- [x] 성공 시 200 + 재로그인 가능
- [x] 성공 시 기존 Refresh Token 삭제(REFRESH_TOKEN_NOT_FOUND로 확인)
- [x] 현재 비밀번호 불일치 시 401 + `INVALID_PASSWORD`(AUTH_003)
- [x] 새 비밀번호가 기존과 동일 시 400 + `SAME_AS_OLD_PASSWORD`(AUTH_008)
- [x] 새 비밀번호 정책 위반 시 400 + 공통 `INVALID_INPUT_VALUE`
- [x] 미인증 요청 시 401 + 공통 `UNAUTHORIZED`
- [x] 탈퇴/정지 회원 시 400 `DELETED_MEMBER` / 403 `SUSPENDED_MEMBER`
- [x] 비밀번호는 BCrypt로 암호화되어 저장됨(DB 직접 조회로 확인)
