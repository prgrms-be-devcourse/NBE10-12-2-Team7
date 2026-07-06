# Postman 시나리오 — 이메일 인증 (발송 · 확인)

> 회원가입 전 이메일 인증(가입 전 인증 방식) 흐름. `POST /api/auth/email-verifications`(발송)과 `POST /api/auth/email-verifications/confirm`(확인) 두 API를 다룬다.
> 실제 `dongne-mysql` + 실제 Gmail SMTP에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 요청해 확인했다(2026-07-06).
> SMTP 자격증명은 `.env`(Git 미추적)로 로컬 셸에만 주입했고, 이 문서·커밋·테스트 코드 어디에도 실제 비밀번호 값은 남기지 않았다.

## 현재 검증 상태
- [x] `email_verifications` 테이블 생성 확인 완료
- [x] 신규 이메일 인증 코드 발송 → 201, 실제 Gmail SMTP로 발송 성공(인증 통과, 메시지 전송 완료) 확인
- [x] 발송 성공 시 `email_verifications`에 row가 남는 것을 DB에서 직접 확인
- [x] 60초 이내 같은 이메일로 재요청 → 429 `EMAIL_VERIFICATION_REQUEST_TOO_SOON` 실제 호출로 확인
- [x] 이미 가입된 이메일 → 409 `DUPLICATE_EMAIL` 실제 호출로 확인
- [x] 이메일 형식 오류 → 400 `INVALID_INPUT_VALUE` 실제 호출로 확인
- [x] 올바른 코드로 확인 → 200 + `verified:true` 실제 호출로 확인
- [x] 코드 불일치 → 400 `INVALID_VERIFICATION_CODE` 실제 호출로 확인
- [x] 코드 만료 → 400 `EXPIRED_VERIFICATION_CODE` 실제 호출로 확인(DB에서 `expires_at`을 과거로 강제 갱신해 재현)
- [x] 인증 요청 이력 없음 → 404 `EMAIL_VERIFICATION_NOT_FOUND` 실제 호출로 확인
- [x] 이미 인증 완료된 건에 다른 코드로 재요청 → 200 + `verified:true`(멱등) 실제 호출로 확인
- [x] 테스트 데이터 정리 및 서버 종료 완료

## 이메일 인증 코드 발송 — POST /api/auth/email-verifications

## 공통
- Method: `POST`
- URL: `/api/auth/email-verifications`
- Headers: `Content-Type: application/json`
- 인증 불필요 (`SecurityConfig` permitAll에 이 경로 1줄 추가 — `global` 영역이라 팀장 리뷰 시 별도 안내 필요)

---

## 1) 성공 — 신규 이메일 인증 코드 발송 (실제 Gmail SMTP 발송)

**Request Body**
```json
{
  "email": "emailverify-realsend@example.com"
}
```

**Response** `201 Created` (실제 호출 결과, 2026-07-06)
```json
{"status":201,"message":"인증 코드가 발송되었습니다.","data":{"email":"emailverify-realsend@example.com","expiresAt":"2026-07-06T11:04:19.3762122"}}
```

DB에도 실제로 저장되었다:
```
mysql> SELECT email, sent_at, expires_at FROM email_verifications WHERE email LIKE 'emailverify%';
email                              sent_at                     expires_at
emailverify-realsend@example.com  2026-07-06 10:59:19.376212  2026-07-06 11:04:19.376212
```

처음 시도했을 때는 두 가지 문제가 있었고, 둘 다 코드 수정으로 해결했다:
1. `MailAuthenticationException: Authentication failed` — Gmail 앱 비밀번호를 화면에 표시된 대로 공백 포함해서 넣어 발생. 공백 제거 후 해결(코드 변경 없음, `.env` 값만 수정).
2. `can't determine local email address` — `SimpleMailMessage`에 발신자(`From`)를 지정하지 않아 로컬 호스트에서 자동 유추를 시도하다 실패. `SmtpEmailSender`에 `spring.mail.username`을 `From`으로 명시 설정해 해결(그룹 2-1 브랜치에 fix 커밋으로 반영).

---

## 2) 실패 — 60초 이내 재요청

1)번과 같은 이메일로 즉시 재요청.

**Request Body**
```json
{
  "email": "emailverify-realsend@example.com"
}
```

**Response** `429 Too Many Requests` (실제 호출 결과, 2026-07-06)
```json
{"status":429,"error":"EMAIL_VERIFICATION_REQUEST_TOO_SOON","message":"인증 코드를 너무 자주 요청했습니다. 잠시 후 다시 시도해주세요.","timestamp":"2026-07-06T10:59:28.5117525"}
```

---

## 3) 실패 — 이미 가입된 이메일

**Request Body**
```json
{
  "email": "emailverify-taken@example.com"
}
```

**Response** `409 Conflict` (실제 호출 결과, 2026-07-06)
```json
{"status":409,"error":"DUPLICATE_EMAIL","message":"이미 사용 중인 이메일입니다.","timestamp":"2026-07-06T10:37:55.6679444"}
```

---

## 4) 실패 — 이메일 형식 오류

**Request Body**
```json
{
  "email": "not-an-email"
}
```

**Response** `400 Bad Request` (실제 호출 결과, 2026-07-06)
```json
{"status":400,"error":"INVALID_INPUT_VALUE","message":"이메일 형식이 올바르지 않습니다.","timestamp":"2026-07-06T10:37:55.7896307"}
```

---

## 이메일 인증 코드 확인 — POST /api/auth/email-verifications/confirm

### 공통
- Method: `POST`
- URL: `/api/auth/email-verifications/confirm`
- Headers: `Content-Type: application/json`
- 인증 불필요 (`SecurityConfig` permitAll에 이 경로도 추가)
- 코드는 이메일로만 전달되므로, 아래 실제 호출은 발송 직후 DB에서 코드 값을 직접 조회해 사용했다(이메일 원문을 직접 열어 보지는 않음).

### 1) 성공 — 올바른 코드로 확인

**Request Body**
```json
{
  "email": "confirm-realtest@example.com",
  "code": "340291"
}
```

**Response** `200 OK` (실제 호출 결과, 2026-07-06)
```json
{"status":200,"message":"이메일 인증이 완료되었습니다.","data":{"email":"confirm-realtest@example.com","verified":true}}
```

### 2) 성공(멱등) — 이미 인증 완료된 건에 다른 코드로 재요청

**Request Body**
```json
{
  "email": "confirm-realtest@example.com",
  "code": "000000"
}
```

**Response** `200 OK` (실제 호출 결과, 2026-07-06 — 1)번 확인 직후 잘못된 코드로 재요청했지만 이미 인증된 건이라 코드 검사 없이 성공)
```json
{"status":200,"message":"이메일 인증이 완료되었습니다.","data":{"email":"confirm-realtest@example.com","verified":true}}
```

### 3) 실패 — 코드 불일치

**Request Body**
```json
{
  "email": "confirm-wrong-real@example.com",
  "code": "999999"
}
```

**Response** `400 Bad Request` (실제 호출 결과, 2026-07-06)
```json
{"status":400,"error":"INVALID_VERIFICATION_CODE","message":"인증 코드가 일치하지 않습니다.","timestamp":"2026-07-06T11:12:02.7763077"}
```

### 4) 실패 — 인증 요청 이력 없음

**Request Body**
```json
{
  "email": "never-requested-real@example.com",
  "code": "123456"
}
```

**Response** `404 Not Found` (실제 호출 결과, 2026-07-06)
```json
{"status":404,"error":"EMAIL_VERIFICATION_NOT_FOUND","message":"이메일 인증 요청 내역을 찾을 수 없습니다.","timestamp":"2026-07-06T11:12:02.904977"}
```

### 5) 실패 — 코드 만료

발송 직후 DB에서 `expires_at`을 1분 전으로 강제 갱신해 만료 상태를 재현했다.

**Request Body**
```json
{
  "email": "confirm-expired-real@example.com",
  "code": "789922"
}
```

**Response** `400 Bad Request` (실제 호출 결과, 2026-07-06)
```json
{"status":400,"error":"EXPIRED_VERIFICATION_CODE","message":"인증 코드가 만료되었습니다. 다시 요청해주세요.","timestamp":"2026-07-06T11:12:25.6951613"}
```

### 6) 실패 — 이메일 형식 오류

**Request Body**
```json
{
  "email": "not-an-email",
  "code": "123456"
}
```

**Response** `400 Bad Request` (실제 호출 결과, 2026-07-06)
```json
{"status":400,"error":"INVALID_INPUT_VALUE","message":"이메일 형식이 올바르지 않습니다.","timestamp":"2026-07-06T11:12:03.0300659"}
```

---

## MySQL / 서버 정리
검증에 사용한 `emailverify-*@example.com`, `confirm-*@example.com`, `never-requested-real@example.com` 행은 `email_verifications`·`members` 테이블에서 모두 삭제했고, 검증용 `bootRun` 프로세스도 종료했다.

## 검증 체크리스트
- [x] 신규 이메일 요청 시 201 + 실제 Gmail SMTP 발송 성공 + DB row 저장
- [x] 60초 이내 재요청 시 429 + `EMAIL_VERIFICATION_REQUEST_TOO_SOON`(AUTH_009)
- [x] 이미 가입된 이메일 시 409 + `DUPLICATE_EMAIL`(AUTH_001)
- [x] 이메일 형식 오류 시 400 + 공통 `INVALID_INPUT_VALUE`
- [x] 발송 실패 시 트랜잭션 롤백으로 row가 남지 않음(자격증명 오류 상태에서 확인, 그룹 2-1 문서 참고)
- [x] 올바른 코드 확인 시 200 + `verified:true`
- [x] 코드 불일치 시 400 + `INVALID_VERIFICATION_CODE`(AUTH_011)
- [x] 코드 만료 시 400 + `EXPIRED_VERIFICATION_CODE`(AUTH_012)
- [x] 인증 요청 이력 없음 시 404 + `EMAIL_VERIFICATION_NOT_FOUND`(AUTH_010)
- [x] 이미 인증 완료된 건 재확인 시 200 + `verified:true`(멱등, 코드 검사 생략)
- [x] 확인 API 이메일 형식 오류 시 400 + 공통 `INVALID_INPUT_VALUE`
