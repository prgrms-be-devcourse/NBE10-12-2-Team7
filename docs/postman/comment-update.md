# Postman 시나리오 — 댓글 수정 (PATCH /api/comments/{commentId})

> 현재 응답 형태와 예외 처리는 자동화 테스트(`CommentControllerTest`, `CommentServiceTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `PATCH`
- URL: `/api/comments/{commentId}`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {accessToken}`
- 인증 필요 (작성자 본인만)
- 사전 조건:
  - 회원가입/로그인으로 Access Token을 발급받는다.
  - 수정할 댓글을 미리 작성해 둔다. (`POST /api/products/{productId}/comments`)

---

## 1) 성공 — 댓글 수정

본인이 작성한 댓글에 `PATCH /api/comments/1`

**Request Body**
```json
{
  "content": "수정된 내용입니다"
}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "댓글이 수정되었습니다.",
  "data": {
    "id": 1,
    "memberId": 1,
    "productId": 1,
    "content": "수정된 내용입니다",
    "createdAt": "2026-06-26T09:00:00",
    "updatedAt": "2026-06-26T09:10:00"
  }
}
```

---

## 2) 실패 — 인증 없음

`Authorization` 헤더 없이 요청한다.

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 3) 실패 — 작성자가 아님

다른 사용자가 작성한 댓글을 수정 시도한다.

**Response** `403 Forbidden`
```json
{
  "status": 403,
  "error": "COMMENT_OWNER_ONLY",
  "message": "댓글 작성자만 처리할 수 있습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 4) 실패 — 존재하지 않는(또는 삭제된) 댓글

`PATCH /api/comments/999999`

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "COMMENT_NOT_FOUND",
  "message": "댓글을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 5) 실패 — 내용 공백

**Request Body**
```json
{
  "content": " "
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_INPUT_VALUE",
  "message": "잘못된 입력값입니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

## 검증 체크리스트

- [x] 인증 없음 시 401 + `UNAUTHORIZED`
- [x] 작성자 본인 수정 시 200 + 수정된 `CommentResponse`
- [x] 작성자가 아니면 403 + `COMMENT_OWNER_ONLY`
- [x] 존재하지 않거나 삭제된 댓글 시 404 + `COMMENT_NOT_FOUND`
- [x] 내용 공백 시 400 + `INVALID_INPUT_VALUE`

## 비고

- 이미 소프트삭제(`deleted_at`)된 댓글은 `findByIdAndDeletedAtIsNull` 조회에서 제외되어 404로 처리된다.
