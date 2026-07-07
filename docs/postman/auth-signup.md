# Postman 시나리오 — 회원가입 (POST /api/auth/signup)

> 1~4번 응답 예시는 실제 MySQL(`docker compose up -d` → `dongne-mysql`)에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 요청해 확인한 값이다(2026-06-25).
> 회원가입은 이제 이메일 인증 완료를 전제로 한다(그룹 2-4, 2026-07-06). 아래 예시들의 이메일은 실제로는 먼저 `POST /api/auth/email-verifications`로 코드를 받고 `POST /api/auth/email-verifications/confirm`으로 확인까지 마쳐야 회원가입이 성공한다. 자세한 흐름은 `docs/postman/auth-email-verification.md`를 참고.
> 회원가입은 이제 이용약관(`termsAgreed`)·개인정보 수집 및 이용 동의(`personalInfoCollectionAgreed`) 동의도 전제로 한다(그룹 2-6, 2026-07-07). 아래 6번 참고. 1~5번 예시의 요청 바디에도 이 두 필드가 반영돼 있다.

## 현재 검증 상태
- [x] MySQL Docker 연결 및 `members` 테이블 생성 확인 완료
- [x] `email`/`nickname` unique 제약 생성 확인 완료
- [x] 8080 포트 충돌 해결 — 점유 프로세스는 `doubleup-dashboard-frontend`가 아니라 IntelliJ로 떠 있던 우리 앱의 stale 인스턴스(DB 연결 끊겨 500 응답 중)였음. 해당 프로세스 종료 후 `./gradlew bootRun`으로 재기동
- [x] `POST /api/auth/signup` 4가지 케이스(성공/이메일중복/닉네임중복/validation) 실제 호출로 확인 완료
- [x] `members` 테이블 저장 데이터 직접 조회로 확인 완료 (비밀번호 BCrypt 해시 저장, unique 인덱스 적용)
- [x] 이메일 인증 선행 조건(그룹 2-4) 4가지 케이스(인증완료 성공/미인증 실패/인증이력없음 실패/이미가입 DUPLICATE_EMAIL 유지) 실제 MySQL + Gmail SMTP 대상 curl로 확인 완료(2026-07-06)
- [x] 약관 동의 선행 조건(그룹 2-6) 3가지 케이스(이용약관 미동의 실패/개인정보 수집·이용 미동의 실패/둘 다 동의 성공) 실제 MySQL 대상 curl로 확인 완료, `member_agreements` 테이블 저장 데이터 직접 조회로 확인 완료(2026-07-07)

## 공통
- Method: `POST`
- URL: `/api/auth/signup`
- Headers: `Content-Type: application/json`
- 인증 불필요 (SecurityConfig permitAll)
- **선행 조건 1**: 요청한 `email`이 이메일 인증(코드 발송 → 확인)을 완료한 상태여야 한다. 미완료 시 400 `EMAIL_NOT_VERIFIED`.
- **선행 조건 2**: `termsAgreed`, `personalInfoCollectionAgreed`가 모두 `true`여야 한다. 검증 순서상 이메일 중복 검사보다 먼저 확인한다(자세한 건 6번 참고).

---

## 1) 성공 — 신규 회원가입

이메일 인증을 먼저 완료했다고 가정한다(아래 5번 참고).

**Request Body**
```json
{
  "email": "test@example.com",
  "password": "password123",
  "nickname": "tester",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `201 Created`
```json
{
  "status": 201,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "memberId": 1,
    "email": "test@example.com",
    "nickname": "tester"
  }
}
```

---

## 2) 실패 — 이메일 중복

같은 `email`로 두 번째 회원가입을 시도.

**Request Body**
```json
{
  "email": "test@example.com",
  "password": "password123",
  "nickname": "another",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `409 Conflict`
```json
{
  "status": 409,
  "error": "DUPLICATE_EMAIL",
  "message": "이미 사용 중인 이메일입니다.",
  "timestamp": "2026-06-25T15:33:36.5479823"
}
```

---

## 3) 실패 — 닉네임 중복

다른 이메일이지만 동일한 `nickname`으로 시도.

**Request Body**
```json
{
  "email": "second@example.com",
  "password": "password123",
  "nickname": "tester",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `409 Conflict`
```json
{
  "status": 409,
  "error": "DUPLICATE_NICKNAME",
  "message": "이미 사용 중인 닉네임입니다.",
  "timestamp": "2026-06-25T15:33:36.6721491"
}
```

---

## 4) 실패(참고) — Validation 오류 (닉네임 빈 값)

`@Valid` 검증 실패는 AUTH 전용 ErrorCode가 아닌 공통 `INVALID_INPUT_VALUE`로 처리된다 (GlobalExceptionHandler).
닉네임 빈 값(`""`)은 `@NotBlank`와 `@Size(min=2)`를 동시에 위반하는데, `GlobalExceptionHandler`는 `FieldError` 목록의 첫 번째 메시지만 반환하고 그 순서는 보장되지 않는다. 실제 호출에서는 `@Size` 메시지가 먼저 나왔다.

**Request Body**
```json
{
  "email": "valid@example.com",
  "password": "password123",
  "nickname": "",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_INPUT_VALUE",
  "message": "닉네임은 2자 이상 20자 이하로 입력해주세요.",
  "timestamp": "2026-06-25T15:33:36.7892775"
}
```

---

## 5) 이메일 인증 선행 조건 (그룹 2-4)

실제 `dongne-mysql` + 실제 Gmail SMTP에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 확인했다(2026-07-06).

### 5-1) 실패 — 인증 요청 이력 자체가 없는 이메일

**Request Body**
```json
{
  "email": "signup-never-verified@example.com",
  "password": "password123!",
  "nickname": "neverVerified",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"EMAIL_NOT_VERIFIED","message":"이메일 인증을 먼저 완료해주세요.","timestamp":"2026-07-06T11:27:04.6289019"}
```

### 5-2) 실패 — 인증 코드는 발송했지만 확인(confirm)하지 않은 이메일

`POST /api/auth/email-verifications`로 코드만 발송받고 `confirm`은 호출하지 않은 상태.

**Request Body**
```json
{
  "email": "signup-unconfirmed@example.com",
  "password": "password123!",
  "nickname": "unconfirmed",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"EMAIL_NOT_VERIFIED","message":"이메일 인증을 먼저 완료해주세요.","timestamp":"2026-07-06T11:27:08.0874765"}
```

### 5-3) 성공 — 인증 완료 이메일

`POST /api/auth/email-verifications` → DB에서 코드 조회 → `POST /api/auth/email-verifications/confirm`까지 마친 뒤 회원가입.

**Request Body**
```json
{
  "email": "signup-verified@example.com",
  "password": "password123!",
  "nickname": "verifiedUser",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `201 Created` (실제 호출 결과)
```json
{"status":201,"message":"회원가입이 완료되었습니다.","data":{"memberId":15,"email":"signup-verified@example.com","nickname":"verifiedUser"}}
```

### 5-4) 실패 — 이미 가입된 이메일은 DUPLICATE_EMAIL 유지

5-3에서 가입 완료한 이메일로 다시 회원가입을 시도. 이메일 인증 여부 검사보다 이메일 중복 검사가 먼저 수행되므로, 인증 완료 상태와 무관하게 기존과 동일한 `DUPLICATE_EMAIL`이 반환된다.

**Request Body**
```json
{
  "email": "signup-verified@example.com",
  "password": "password123!",
  "nickname": "anotherNick",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `409 Conflict` (실제 호출 결과)
```json
{"status":409,"error":"DUPLICATE_EMAIL","message":"이미 사용 중인 이메일입니다.","timestamp":"2026-07-06T11:27:47.2786075"}
```

---

## 6) 약관 동의 선행 조건 (그룹 2-6)

실제 `dongne-mysql`에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 확인했다(2026-07-07). 이메일 인증 검사보다 먼저 수행되므로, 인증 완료 여부와 무관하게 동의 여부만으로 먼저 걸린다.

### 6-1) 실패 — 이용약관 미동의

**Request Body**
```json
{
  "email": "signup-consent-postman2@example.com",
  "password": "password123!",
  "nickname": "consentTester2",
  "termsAgreed": false,
  "personalInfoCollectionAgreed": true
}
```

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"TERMS_NOT_AGREED","message":"이용약관에 동의해야 합니다.","timestamp":"2026-07-07T12:34:31.4493519"}
```

### 6-2) 실패 — 개인정보 수집 및 이용 미동의

**Request Body**
```json
{
  "email": "signup-consent-postman3@example.com",
  "password": "password123!",
  "nickname": "consentTester3",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": false
}
```

**Response** `400 Bad Request` (실제 호출 결과)
```json
{"status":400,"error":"PERSONAL_INFO_COLLECTION_NOT_AGREED","message":"개인정보 수집 및 이용에 동의해야 합니다.","timestamp":"2026-07-07T12:34:51.3704308"}
```

### 6-3) 성공 — 둘 다 동의 + `member_agreements` 저장 확인

이메일 인증까지 완료한 상태에서 둘 다 동의하고 가입.

**Request Body**
```json
{
  "email": "signup-consent-success@example.com",
  "password": "password123!",
  "nickname": "consentSuccess",
  "termsAgreed": true,
  "personalInfoCollectionAgreed": true
}
```

**Response** `201 Created` (실제 호출 결과)
```json
{"status":201,"message":"회원가입이 완료되었습니다.","data":{"memberId":23,"email":"signup-consent-success@example.com","nickname":"consentSuccess"}}
```

가입 직후 `member_agreements` 테이블을 직접 조회해 항목별로 1건씩(총 2건) 저장됐는지 확인:
```
mysql> SELECT member_id, agreement_type, version, agreed_at FROM member_agreements WHERE member_id=23;
member_id  agreement_type            version  agreed_at
23         TERMS_OF_SERVICE          v1.0     2026-07-07 12:35:03.173188
23         PERSONAL_INFO_COLLECTION  v1.0     2026-07-07 12:35:03.173188
```

---

## MySQL 직접 확인

위 1~3번 호출 후 `dongne-mysql` 컨테이너에 직접 접속해 확인한 결과 (중복/validation 요청은 실제로 insert되지 않고 1건만 저장됨):

```
mysql> SELECT id, email, nickname, password, role, status, deleted_at FROM members;
id  email              nickname  password                                                      role       status  deleted_at
1   test@example.com   tester    $2a$10$yYcFgLIFWc1Y0zyPs0n6Muoh4BY4Xl2xOIvHwCJEreyaSw9J2QrZu    ROLE_USER  ACTIVE  NULL

mysql> SHOW INDEX FROM members WHERE Key_name != 'PRIMARY';
Table    Non_unique  Key_name                      Column_name
members  0           UK9d30a9u1qpg8eou0otgkwrp5d   email
members  0           UKe6u9u9ypoc7oldnpxdjwcdx3    nickname
```

- 비밀번호가 평문이 아닌 BCrypt 해시(`$2a$10$...`)로 저장됨.
- `email`, `nickname` 모두 `Non_unique = 0`(= UNIQUE 인덱스) 확인.
- 중복/validation 실패 요청 3건은 테이블에 반영되지 않고 성공 1건만 존재 — 트랜잭션 롤백 정상 동작.

## 검증 체크리스트
- [x] 성공 시 201 + `ApiResponse` 포맷 + 비밀번호는 응답에 노출되지 않음
- [x] 이메일 중복 시 409 + `DUPLICATE_EMAIL`(AUTH_001)
- [x] 닉네임 중복 시 409 + `DUPLICATE_NICKNAME`(AUTH_002)
- [x] 필수값 누락 시 400 + 공통 `INVALID_INPUT_VALUE`
- [x] 비밀번호는 BCrypt로 암호화되어 저장됨 (DB 평문 저장 없음, MySQL 직접 조회로 확인)
- [x] `email`/`nickname` UNIQUE 인덱스가 실제 DB에 생성됨 (MySQL `SHOW INDEX`로 확인)
- [x] 이메일 인증 미완료 시 400 + `EMAIL_NOT_VERIFIED`(AUTH_013) — 인증 이력 없음/미확인 두 경우 모두 확인
- [x] 이메일 인증 완료 시 201 정상 가입
- [x] 이미 가입된 이메일은 인증 여부와 무관하게 기존 409 `DUPLICATE_EMAIL` 유지(이메일 중복 검사가 먼저 수행됨)
- [x] 이용약관 미동의 시 400 + `TERMS_NOT_AGREED`(AUTH_017)
- [x] 개인정보 수집 및 이용 미동의 시 400 + `PERSONAL_INFO_COLLECTION_NOT_AGREED`(AUTH_018)
- [x] 둘 다 동의 시 201 정상 가입 + `member_agreements`에 `TERMS_OF_SERVICE`/`PERSONAL_INFO_COLLECTION` 각 1건씩 저장(MySQL 직접 조회로 확인)
