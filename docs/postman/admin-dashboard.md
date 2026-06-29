# Postman 시나리오 — 관리자 대시보드 (GET /api/admin/dashboard)

> 아래는 **예상 응답**이다. 실제 MySQL 서버(`docker compose up -d` → `./gradlew bootRun`) 기동 후 Postman으로 확인하고, 집계 값·`timestamp`를 실제 값으로 갱신한 뒤 맨 아래 체크리스트를 체크한다.
> 관리자 계정 시드(PR 1)가 아직 없으므로 토큰은 **수동 승격**으로 발급한다(사전 조건).

## 공통
- Method: `GET`
- URL: `/api/admin/dashboard`
- 인증: **ROLE_ADMIN 필요** (`Authorization: Bearer {adminAccessToken}`)
  - 일반 사용자(ROLE_USER) → 403 `FORBIDDEN`, 미인증 → 401 `UNAUTHORIZED`
- 집계 항목: 전체 회원 수, 전체 상품 수, 전체 신고 수, 접수 대기 신고 수(`RECEIVED`), 전체 댓글 수.

### 사전 조건 — 관리자 토큰
```
admin 가입 → MySQL: UPDATE members SET role='ROLE_ADMIN' WHERE email='admin@example.com'; → 로그인 → accessToken
```

---

# GET /api/admin/dashboard — 관리 대시보드 집계

## 1) 성공 — 관리자

**Request**
```
GET /api/admin/dashboard
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "totalMembers": 42,
    "totalProducts": 18,
    "totalReports": 5,
    "pendingReports": 3,
    "totalComments": 27
  }
}
```
> `pendingReports`는 상태가 `RECEIVED`인 신고 수(검토 대기). 값은 실제 DB 데이터에 따라 달라진다.

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

## 검증 체크리스트
- [ ] 관리자 성공 200 + `data`에 totalMembers/totalProducts/totalReports/pendingReports/totalComments 포함
- [ ] 각 값이 실제 건수와 일치 (회원·상품·신고·댓글 몇 건 만들고 증감 확인)
- [ ] `pendingReports`가 `RECEIVED` 상태 신고 수와 일치
- [ ] 일반 사용자 → 403 `FORBIDDEN`(COMMON_004)
- [ ] 토큰 없음 → 401 `UNAUTHORIZED`(COMMON_003)
