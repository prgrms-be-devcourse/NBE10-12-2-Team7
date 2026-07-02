# 01. Auth (회원가입·로그인) — 담당: 김대연

대상: `AuthService`, `AuthController`
관련 ErrorCode: `DUPLICATE_EMAIL`, `DUPLICATE_NICKNAME`, `INVALID_PASSWORD`, `MEMBER_NOT_FOUND`, `DELETED_MEMBER`, `SUSPENDED_MEMBER`

> 계층 표기·픽스처·인가 셋업은 [00-test-strategy.md](00-test-strategy.md) 참고.

## 핵심 로직 요약 (테스트 전 확인)

- **검증 순서(signup)**: `existsByEmail` → `existsByNickname` → 인코딩·save → (save 시 `DataIntegrityViolationException`이면) `resolveDuplicateException`으로 재조회.
- **검증 순서(login)**: `findByEmail`(없으면 MEMBER_NOT_FOUND) → **DELETED 체크 → SUSPENDED 체크 → 비밀번호 매칭**. 즉 상태가 비번보다 **먼저** 검사됨.
- 회원가입은 항상 `ROLE_USER` / `ACTIVE` / **BCrypt 인코딩** 저장.
- 로그인 성공 → `jwtTokenProvider.createAccessToken(id, role.name())` 토큰 반환.

## 회원가입 (POST /api/auth/signup) — 비인증 허용

| ID | 계층 | 시나리오 (Given→When) | 기대 결과 |
|---|---|---|---|
| AU-01 | S | 신규 이메일·닉네임 → signup | 저장 성공, ROLE_USER·ACTIVE, **저장 비번 ≠ 원문**(인코딩 호출), SignupResponse 반환 |
| AU-02 | S | `existsByEmail=true` → signup | `DUPLICATE_EMAIL` (409) |
| AU-03 | S | 이메일은 유니크, `existsByNickname=true` → signup | `DUPLICATE_NICKNAME` (409) |
| AU-04 | S | 사전체크 통과했으나 save에서 `DataIntegrityViolationException`, 재조회 시 **이메일** 중복 | `DUPLICATE_EMAIL` |
| AU-05 | S | 〃, 재조회 시 **닉네임** 중복 | `DUPLICATE_NICKNAME` |
| AU-06 | S | 〃, 재조회 시 이메일·닉네임 모두 정상(원인 불명) | 원본 `DataIntegrityViolationException` 재전파 |
| AU-07 | C | 정상 요청 | 201, body에 SignupResponse |
| AU-08 | C | email 공백/형식오류(`@Email`) | 400 / `COMMON_002` |
| AU-09 | C | password 공백 / 7자 / 21자(`@Size 8~20`) | 400 / `COMMON_002` |
| AU-10 | C | nickname 공백 / 1자 / 21자(`@Size 2~20`) | 400 / `COMMON_002` |
| AU-11 | C | 경계값: password 8자·20자, nickname 2자·20자 | 통과(서비스 호출됨) |

## 로그인 (POST /api/auth/login) — 비인증 허용

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| AU-20 | S | 존재·ACTIVE·비번 일치 → login | 토큰 발급(`createAccessToken(id, "ROLE_USER")` 호출), LoginResponse 반환 |
| AU-21 | S | 이메일 없음 | `MEMBER_NOT_FOUND` (404) |
| AU-22 | S | status=DELETED | `DELETED_MEMBER` (400) |
| AU-23 | S | status=SUSPENDED | `SUSPENDED_MEMBER` (403) |
| AU-24 | S | ACTIVE·비번 불일치 | `INVALID_PASSWORD` (401) |
| AU-25 | S | ⭐ **DELETED + 비번도 틀림** | `DELETED_MEMBER` (상태가 비번보다 먼저 검사됨 — 순서 검증) |
| AU-26 | S | ADMIN 계정 로그인 | 토큰에 role="ROLE_ADMIN" 전달되는지(`createAccessToken` 인자 검증) |
| AU-27 | C | 정상 | 200, body에 token |
| AU-28 | C | email/password 공백(`@NotBlank`) | 400 / `COMMON_002` |

## 인가

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| AU-30 | C/I | 비인증으로 signup·login 호출 | 허용(permitAll) |

## 비고 / 주의

- AU-04~06의 race condition 분기는 mock으로 `save`가 예외를 던지도록 설정하고, 이어지는 `existsByEmail/Nickname` stub을 조합해 검증.
- AU-25는 **버그 회귀 방지**의 핵심: 상태 검사가 비번 검사 뒤로 가면 정지/탈퇴 회원에게 "비번 틀림"을 노출하게 됨.
- 토큰 문자열 자체의 유효성(서명/만료)은 auth가 아니라 `JwtTokenProvider` 단위 테스트(있으면)에서. 여기서는 "올바른 인자로 호출됐는가"까지만.
