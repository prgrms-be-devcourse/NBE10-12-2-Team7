# Postman 시나리오 — 회원가입 (POST /api/auth/signup)

> 1~4번 응답 예시는 실제 MySQL(`docker compose up -d` → `dongne-mysql`)에 연결된 서버(`./gradlew bootRun`)에 curl로 직접 요청해 확인한 값이다(2026-06-25).

## 현재 검증 상태
- [x] MySQL Docker 연결 및 `members` 테이블 생성 확인 완료
- [x] `email`/`nickname` unique 제약 생성 확인 완료
- [x] 8080 포트 충돌 해결 — 점유 프로세스는 `doubleup-dashboard-frontend`가 아니라 IntelliJ로 떠 있던 우리 앱의 stale 인스턴스(DB 연결 끊겨 500 응답 중)였음. 해당 프로세스 종료 후 `./gradlew bootRun`으로 재기동
- [x] `POST /api/auth/signup` 4가지 케이스(성공/이메일중복/닉네임중복/validation) 실제 호출로 확인 완료
- [x] `members` 테이블 저장 데이터 직접 조회로 확인 완료 (비밀번호 BCrypt 해시 저장, unique 인덱스 적용)

## 공통
- Method: `POST`
- URL: `/api/auth/signup`
- Headers: `Content-Type: application/json`
- 인증 불필요 (SecurityConfig permitAll)

---

## 1) 성공 — 신규 회원가입

**Request Body**
```json
{
  "email": "test@example.com",
  "password": "password123",
  "nickname": "tester"
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
  "nickname": "another"
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
  "nickname": "tester"
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
  "nickname": ""
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
