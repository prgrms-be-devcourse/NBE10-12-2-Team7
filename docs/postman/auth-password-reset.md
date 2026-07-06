# Postman 시나리오 — 비밀번호 찾기 (재설정 요청 · 확인)

> 실제 `dongne-mysql` + 실제 Gmail SMTP에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 요청해 확인했다(2026-07-06).
> `POST /api/auth/password-resets`(요청)과 `POST /api/auth/password-resets/confirm`(확인) 두 API를 다룬다.

## 설계 요약
- **계정 존재 여부 비노출**: 요청 API는 가입 여부·탈퇴 여부와 무관하게 항상 동일한 200 + 중립 메시지를 반환한다. 실제 토큰 발급·메일 발송은 활성/정지 회원에게만 조용히 수행된다.
- **토큰 유효시간**: 30분, **1회용**(사용 후 재사용 불가), 회원당 1개만 유지(재요청 시 교체).
- **재요청 쿨다운**: 60초. 쿨다운 중 재요청도 동일한 200 응답을 반환하고 조용히 무시한다(응답으로 쿨다운 여부를 구분할 수 없게 함).
- **확인 성공 시**: 기존 Refresh Token 삭제(단일 세션 정책이라 재로그인 필요) — 그룹 3(비밀번호 변경)과 동일한 보안 정책.

## 현재 검증 상태
- [x] 가입된 이메일로 요청 → 200 + 실제 Gmail SMTP 발송 확인
- [x] 가입되지 않은 이메일로 요청 → 200 + **동일한 메시지**, 발송 없음(계정 존재 노출 방지 확인)
- [x] 유효한 토큰으로 확인 → 200, 새 비밀번호로 로그인 성공 확인
- [x] 같은 토큰 재사용 시도(1회용 소진) → 400 `INVALID_RESET_TOKEN`
- [x] 존재하지 않는 토큰 → 400 `INVALID_RESET_TOKEN`
- [x] 만료된(미사용) 토큰 → 400 `EXPIRED_RESET_TOKEN`
- [x] 테스트 데이터 정리 및 서버 종료 완료

## 공통
- Method: `POST`
- Headers: `Content-Type: application/json`
- 인증 불필요 (`SecurityConfig` permitAll에 두 경로 추가 — `global` 영역이라 팀장 리뷰 시 별도 안내 필요)

---

## 이메일 요청 — POST /api/auth/password-resets

### 1) 성공 — 가입된 이메일

**Request Body**
```json
{
  "email": "pwreset-real@example.com"
}
```

**Response** `200 OK` (실제 호출 결과)
```json
{"status":200,"message":"해당 이메일로 가입된 계정이 있다면 비밀번호 재설정 메일을 발송했습니다."}
```

### 2) 성공(동일 응답) — 가입되지 않은 이메일

**Request Body**
```json
{
  "email": "never-registered@example.com"
}
```

**Response** `200 OK` (실제 호출 결과 — 1)번과 완전히 동일한 응답, 실제로는 발송하지 않음)
```json
{"status":200,"message":"해당 이메일로 가입된 계정이 있다면 비밀번호 재설정 메일을 발송했습니다."}
```

### 3) 이메일 형식 오류

**Request Body**
```json
{
  "email": "not-an-email"
}
```

**Response** `400 Bad Request`
```json
{"status":400,"error":"INVALID_INPUT_VALUE","message":"이메일 형식이 올바르지 않습니다.","timestamp":"..."}
```

---

## 재설정 확인 — POST /api/auth/password-resets/confirm

토큰은 이메일로만 전달되므로, 아래 실제 호출은 요청 직후 DB에서 토큰 값을 직접 조회해 사용했다(이메일 원문을 직접 열어 보지는 않음).

### 4) 성공 — 유효한 토큰

**Request Body**
```json
{
  "token": "OIX1ZO6DgIS4aHJfRQSAsIRyHDwolv8IbXatgKN4DqE",
  "newPassword": "resetNewPassword123!"
}
```

**Response** `200 OK` (실제 호출 결과)
```json
{"status":200,"message":"비밀번호가 재설정되었습니다."}
```

재설정 후 새 비밀번호로 즉시 로그인 성공을 확인했다:
```json
{"status":200,"message":"로그인이 완료되었습니다.","data":{"accessToken":"..."}}
```

### 5) 실패 — 같은 토큰 재사용(1회용 소진)

**Response** `400 Bad Request` (실제 호출 결과, 4)번 토큰을 다시 사용)
```json
{"status":400,"error":"INVALID_RESET_TOKEN","message":"유효하지 않은 비밀번호 재설정 링크입니다. 다시 요청해주세요.","timestamp":"2026-07-06T14:39:14.285679"}
```

### 6) 실패 — 존재하지 않는 토큰

**Request Body**
```json
{
  "token": "totally-unknown-token",
  "newPassword": "somePassword123!"
}
```

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"INVALID_RESET_TOKEN","message":"유효하지 않은 비밀번호 재설정 링크입니다. 다시 요청해주세요.","timestamp":"2026-07-06T14:39:25.0062321"}
```

### 7) 실패 — 만료된(미사용) 토큰

DB에서 `expires_at`을 1분 전으로 강제 갱신해 만료 상태를 재현했다(사용되지 않은 토큰으로 별도 확인 — 이미 사용된 토큰에 만료를 적용하면 `INVALID_RESET_TOKEN`이 먼저 반환됨에 유의).

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"EXPIRED_RESET_TOKEN","message":"비밀번호 재설정 링크가 만료되었습니다. 다시 요청해주세요.","timestamp":"2026-07-06T14:39:52.2755743"}
```

---

## MySQL / 서버 정리
검증에 사용한 `pwreset-*@example.com` 행은 `password_reset_tokens`·`email_verifications`·`members` 테이블에서 모두 삭제했고, 검증용 `bootRun` 프로세스도 종료했다.

## 검증 체크리스트
- [x] 가입된 이메일 요청 시 200 + 실제 Gmail SMTP 발송 성공
- [x] 가입되지 않은 이메일 요청 시 200 + 동일 메시지(계정 존재 비노출), 발송 없음
- [x] 이메일 형식 오류 시 400 + 공통 `INVALID_INPUT_VALUE`
- [x] 유효한 토큰 확인 시 200 + 새 비밀번호로 로그인 성공
- [x] 토큰 재사용(1회용 소진) 시 400 + `INVALID_RESET_TOKEN`(AUTH_015)
- [x] 존재하지 않는 토큰 시 400 + `INVALID_RESET_TOKEN`(AUTH_015)
- [x] 만료된 토큰 시 400 + `EXPIRED_RESET_TOKEN`(AUTH_016)
- [x] 새 비밀번호 정책 위반 시 400 + 공통 `INVALID_INPUT_VALUE`
