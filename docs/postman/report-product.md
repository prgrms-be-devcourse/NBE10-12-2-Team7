# Postman 시나리오 — 상품 신고 (POST /api/products/{productId}/reports)

## 사전 준비
- `docker compose up -d` 로 MySQL 기동
- `./gradlew bootRun` 으로 서버 기동 (포트 8080)
- 회원가입 + 로그인으로 JWT 토큰 발급 후 `Authorization: Bearer {accessToken}` 헤더 설정

## 현재 검증 상태
- [x] MySQL Docker 연결 및 `reports` 테이블 생성 확인 완료
- [x] `POST /api/products/{productId}/reports` 성공/실패 케이스 실제 호출로 확인 완료

## 공통
- Method: `POST`
- URL: `/api/products/{productId}/reports`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {accessToken}`

---

## 1) 성공 — 상품 신고 접수

**Request**
```
POST /api/products/1/reports
```
```json
{
  "reason": "FAKE_ITEM",
  "content": "실제로 없는 상품입니다."
}
```

**Actual Response** `201 Created`
```json
{
  "status": 201,
  "message": "신고가 접수되었습니다.",
  "data": {
    "reportId": 1,
    "reportType": "PRODUCT",
    "reason": "FAKE_ITEM",
    "status": "RECEIVED",
    "createdAt": "2026-06-26T11:47:46.9695065"
  }
}
```

---

## 2) 실패 — 중복 신고

같은 상품을 동일한 사용자가 두 번째 신고 시도.

**Request**
```
POST /api/products/1/reports
```
```json
{
  "reason": "FRAUD_SUSPECTED",
  "content": "사기 의심됩니다."
}
```

**Actual Response** `409 Conflict`
```json
{
  "status": 409,
  "error": "DUPLICATE_REPORT",
  "message": "이미 신고한 대상입니다.",
  "timestamp": "2026-06-26T11:49:56.5811769"
}
```

---

## 3) 실패 — 인증 없이 신고 시도

`Authorization` 헤더 없이 요청.

**Actual Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-26T11:54:48.6878365"
}
```

---

## 4) 실패 — 신고 사유 누락 (@Valid)

`reason` 필드 없이 요청.

**Request**
```json
{
  "content": "내용만 있고 사유가 없는 경우"
}
```

**Actual Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_INPUT_VALUE",
  "message": "신고 사유는 필수입니다.",
  "timestamp": "2026-06-26T11:56:25.5680334"
}
```

---

## 검증 체크리스트
- [x] 성공 시 201 + `ReportResponse` 포맷 + `status = RECEIVED`
- [x] 중복 신고 시 409 + `DUPLICATE_REPORT`
- [x] 인증 없이 요청 시 401 + `UNAUTHORIZED`
- [x] `reason` 누락 시 400 + `INVALID_INPUT_VALUE`
- [x] `reports` 테이블에 신고 데이터 저장 확인 (MySQL 직접 조회)

## 미검증 항목 (Product Entity 미머지로 인해 보류)
- 존재하지 않는 상품 신고 시 `PRODUCT_NOT_FOUND` — Product Entity 머지 후 구현 예정
- 본인 상품 신고 시 `CANNOT_REPORT_OWN_PRODUCT` — Product Entity 머지 후 구현 예정
