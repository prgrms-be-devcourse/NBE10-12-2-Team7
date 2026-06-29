# Postman 시나리오 — 관리자 댓글 관리 (GET /api/admin/comments, DELETE /api/admin/comments/{commentId})

> 아래는 **예상 응답**이다. 실제 MySQL 서버(`docker compose up -d` → `./gradlew bootRun`) 기동 후 Postman으로 각 케이스를 확인하고, `timestamp`·`commentId` 등을 실제 값으로 갱신한 뒤 맨 아래 체크리스트를 체크한다.
> 관리자 계정 시드(PR 1)가 아직 없으므로 토큰은 **수동 승격**으로 발급한다(사전 조건). PR 1 머지 후에는 시드 관리자 계정으로 로그인.

## 공통
- URL: `/api/admin/comments`, `/api/admin/comments/{commentId}`
- 인증: **ROLE_ADMIN 필요** (`Authorization: Bearer {adminAccessToken}`)
  - 일반 사용자(ROLE_USER) → 403 `FORBIDDEN`, 미인증 → 401 `UNAUTHORIZED`
- 목록은 **삭제 여부와 무관하게 전체 댓글**을 반환한다.
- 관리자는 **작성자가 아니어도** 댓글을 소프트 삭제할 수 있다.

### 사전 조건 — 관리자 토큰 / 검증용 댓글
```
1. admin 가입 → MySQL: UPDATE members SET role='ROLE_ADMIN' WHERE email='admin@example.com'; → 로그인 → accessToken
2. 검증용 댓글: 사용자 로그인 후 POST /api/products/{productId}/comments 로 댓글 등록
```

---

# GET /api/admin/comments — 댓글 목록 조회

## 1) 성공 — 관리자 (삭제 포함 전체)

**Request**
```
GET /api/admin/comments
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "commentId": 1,
      "memberId": 2,
      "productId": 5,
      "content": "정상 댓글입니다.",
      "deletedAt": null,
      "createdAt": "2026-06-28T10:00:00"
    },
    {
      "commentId": 2,
      "memberId": 3,
      "productId": 5,
      "content": "부적절하여 삭제된 댓글",
      "deletedAt": "2026-06-28T11:00:00",
      "createdAt": "2026-06-28T10:01:00"
    }
  ]
}
```
> 삭제된 댓글(`commentId: 2`, `deletedAt != null`)도 포함된다.

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

# DELETE /api/admin/comments/{commentId} — 댓글 소프트 삭제

## 1) 성공 — 관리자

**Request**
```
DELETE /api/admin/comments/1
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{ "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
```
> 소프트 삭제이므로 `deleted_at`만 기록되고 행은 남는다. 이후 목록 조회 시 해당 댓글은 `deletedAt != null`로 보인다.

---

## 2) 실패 — 존재하지 않는(또는 이미 삭제된) 댓글

**Request**
```
DELETE /api/admin/comments/999999
Authorization: Bearer {adminAccessToken}
```

**Response** `404 Not Found`
```json
{ "status": 404, "error": "COMMENT_NOT_FOUND", "message": "댓글을 찾을 수 없습니다.", "timestamp": "2026-06-28T10:07:00.000000" }
```

## 3) 실패 — 권한/인증 (목록과 동일)
- 일반 사용자 → `403 FORBIDDEN` / 토큰 없음 → `401 UNAUTHORIZED`

---

## 검증 체크리스트

### GET /api/admin/comments
- [ ] 관리자 성공 200 + `data` 배열(삭제 포함 전체)
- [ ] 각 원소에 commentId/memberId/productId/content/deletedAt/createdAt 포함
- [ ] 일반 사용자 → 403 `FORBIDDEN`(COMMON_004)
- [ ] 토큰 없음 → 401 `UNAUTHORIZED`(COMMON_003)

### DELETE /api/admin/comments/{commentId}
- [ ] 관리자 성공 200 (data 없음) + 소프트 삭제(`deletedAt` 기록)
- [ ] 없는(이미 삭제) 댓글 → 404 `COMMENT_NOT_FOUND`(COMMENT_001)
- [ ] 일반 사용자 → 403 `FORBIDDEN` / 토큰 없음 → 401 `UNAUTHORIZED`
