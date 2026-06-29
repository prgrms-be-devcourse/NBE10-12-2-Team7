# Postman 시나리오 — 관리자 신고 관리 (GET·PATCH /api/admin/reports)

> 아래는 **예상 응답**이다. 실제 MySQL 서버(`docker compose up -d` → `./gradlew bootRun`) 기동 후 Postman으로 각 케이스를 확인하고, `timestamp`·`reportId` 등을 실제 값으로 갱신한 뒤 맨 아래 체크리스트를 체크한다.
> 관리자 계정 시드(PR 1)가 적용되어, 시드 계정으로 로그인하면 ROLE_ADMIN 토큰을 받는다(사전 조건).

## 공통
- Method: `GET`
- URL: `/api/admin/reports`, `/api/admin/reports/{reportId}`
- 인증: **ROLE_ADMIN 필요** (`Authorization: Bearer {adminAccessToken}`)
  - 일반 사용자(ROLE_USER) → 403 `FORBIDDEN`, 미인증 → 401 `UNAUTHORIZED`
- 목록은 전체 신고를 반환한다. 정렬 미지정(기본 `id` 순).

### 사전 조건 — 관리자 토큰 / 검증용 신고
```
1. 관리자 토큰: 시드 계정 admin@dongnemarket.com / admin1234! 로 로그인 → data.accessToken
   (시드 없는 환경이면 일반 가입 후 UPDATE members SET role='ROLE_ADMIN' 승격 → 로그인)
2. 검증용 신고: 사용자 로그인 후
   POST /api/products/{productId}/reports  (상품 신고)  또는
   POST /api/members/{memberId}/reports    (회원 신고)
```

---

# GET /api/admin/reports — 신고 목록 조회

## 1) 성공 — 관리자 (전체)

**Request**
```
GET /api/admin/reports
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "reportId": 1,
      "reporterId": 2,
      "targetMemberId": null,
      "targetProductId": 10,
      "reportType": "PRODUCT",
      "reason": "FAKE_ITEM",
      "content": "가품으로 의심됩니다.",
      "status": "RECEIVED",
      "createdAt": "2026-06-28T10:00:00"
    },
    {
      "reportId": 2,
      "reporterId": 4,
      "targetMemberId": 7,
      "targetProductId": null,
      "reportType": "MEMBER",
      "reason": "FRAUD_SUSPECTED",
      "content": "사기 의심 사용자입니다.",
      "status": "RECEIVED",
      "createdAt": "2026-06-28T10:01:00"
    }
  ]
}
```
> 상품 신고는 `targetProductId`, 회원 신고는 `targetMemberId`만 채워진다(`reportType`으로 구분).

---

## 2) 실패 — 일반 사용자(ROLE_USER) 접근

**Response** `403 Forbidden`
```json
{ "status": 403, "error": "FORBIDDEN", "message": "접근 권한이 없습니다.", "timestamp": "2026-06-28T10:05:00.000000" }
```

## 3) 실패 — 토큰 없음

**Response** `401 Unauthorized`
```json
{ "status": 401, "error": "UNAUTHORIZED", "message": "인증이 필요합니다.", "timestamp": "2026-06-28T10:06:00.000000" }
```

---

# GET /api/admin/reports/{reportId} — 신고 상세 조회

## 1) 성공 — 관리자

**Request**
```
GET /api/admin/reports/1
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "reportId": 1,
    "reporterId": 2,
    "targetMemberId": null,
    "targetProductId": 10,
    "reportType": "PRODUCT",
    "reason": "FAKE_ITEM",
    "content": "가품으로 의심됩니다.",
    "status": "RECEIVED",
    "createdAt": "2026-06-28T10:00:00"
  }
}
```

---

## 2) 실패 — 존재하지 않는 신고

**Request**
```
GET /api/admin/reports/999999
Authorization: Bearer {adminAccessToken}
```

**Response** `404 Not Found`
```json
{ "status": 404, "error": "REPORT_NOT_FOUND", "message": "신고 내역을 찾을 수 없습니다.", "timestamp": "2026-06-28T10:07:00.000000" }
```

## 3) 실패 — 권한/인증 (목록과 동일)
- 일반 사용자 → `403 FORBIDDEN` / 토큰 없음 → `401 UNAUTHORIZED`

---

# PATCH /api/admin/reports/{reportId}/status — 신고 상태 변경

> 관리자가 신고 상태를 `RECEIVED`/`REVIEWING`/`COMPLETED`/`REJECTED` 로 변경한다. (전이 검증 없음 — 유효한 값이면 변경)

## 1) 성공 — 관리자

**Request**
```
PATCH /api/admin/reports/1/status
Authorization: Bearer {adminAccessToken}
Content-Type: application/json
```
```json
{ "status": "COMPLETED" }
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "reportId": 1,
    "reporterId": 2,
    "targetMemberId": null,
    "targetProductId": 10,
    "reportType": "PRODUCT",
    "reason": "FAKE_ITEM",
    "content": "가품으로 의심됩니다.",
    "status": "COMPLETED",
    "createdAt": "2026-06-28T10:00:00"
  }
}
```

## 2) 실패 — 잘못된 상태 값

**Request Body**
```json
{ "status": "INVALID" }
```
**Response** `400 Bad Request`
```json
{ "status": 400, "error": "INVALID_REPORT_STATUS", "message": "잘못된 신고 상태 값입니다.", "timestamp": "2026-06-28T10:08:00.000000" }
```

## 3) 실패 — 그 외
- 존재하지 않는 신고 → `404 REPORT_NOT_FOUND`
- 일반 사용자 → `403 FORBIDDEN` / 토큰 없음 → `401 UNAUTHORIZED`

---

## 검증 체크리스트

### GET /api/admin/reports
- [ ] 관리자 성공 200 + `data` 배열(전체 신고)
- [ ] 각 원소에 reportId/reporterId/targetMemberId/targetProductId/reportType/reason/content/status/createdAt 포함
- [ ] 일반 사용자 → 403 `FORBIDDEN`(COMMON_004)
- [ ] 토큰 없음 → 401 `UNAUTHORIZED`(COMMON_003)

### GET /api/admin/reports/{reportId}
- [ ] 관리자 성공 200 + 해당 신고 정보
- [ ] 없는 신고 → 404 `REPORT_NOT_FOUND`(REPORT_001)
- [ ] 일반 사용자 → 403 `FORBIDDEN` / 토큰 없음 → 401 `UNAUTHORIZED`

### PATCH /api/admin/reports/{reportId}/status
- [ ] 관리자 성공 200 + 변경된 status 반환
- [ ] 잘못된 상태 값 → 400 + `INVALID_REPORT_STATUS`(ADMIN_003)
- [ ] 없는 신고 → 404 + `REPORT_NOT_FOUND`(REPORT_001)
- [ ] 일반 사용자 → 403 `FORBIDDEN` / 토큰 없음 → 401 `UNAUTHORIZED`
