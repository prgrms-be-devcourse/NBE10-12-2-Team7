# Postman 시나리오 — 댓글 목록 조회 (GET /api/products/{productId}/comments)

> 현재 응답 형태와 예외 처리는 자동화 테스트(`CommentControllerTest`, `CommentServiceTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `GET`
- URL: `/api/products/{productId}/comments`
- Headers: 없음
- **인증 불필요** (비로그인 사용자도 조회 가능)
- 삭제(소프트 삭제)된 댓글은 목록에서 제외된다 (`deleted_at IS NULL`).
- 정렬: 작성(생성)순 오름차순 (`created_at ASC`).
- 사전 조건:
  - 조회할 상품이 존재해야 한다.
  - 목록을 확인하려면 댓글을 미리 작성해 둔다. (`POST /api/products/{productId}/comments`)

---

## 1) 성공 — 댓글 목록 조회

댓글이 있는 상품에 `GET /api/products/1/comments` (토큰 없이도 가능)

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 1,
      "memberId": 1,
      "productId": 1,
      "content": "첫 번째 댓글",
      "createdAt": "2026-06-27T09:00:00",
      "updatedAt": "2026-06-27T09:00:00"
    },
    {
      "id": 2,
      "memberId": 2,
      "productId": 1,
      "content": "두 번째 댓글",
      "createdAt": "2026-06-27T09:01:00",
      "updatedAt": "2026-06-27T09:01:00"
    }
  ]
}
```

---

## 2) 성공 — 댓글이 없는 상품 (빈 목록)

댓글이 하나도 없는 상품에 `GET /api/products/1/comments`

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": []
}
```

---

## 3) 실패 — 존재하지 않는 상품

`GET /api/products/999999/comments`

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-27T09:00:00"
}
```

---

## 4) 실패 — 숨김(hidden) 처리된 상품

관리자가 숨김 처리한 상품에 `GET /api/products/{productId}/comments` 를 요청한다.

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-27T09:00:00"
}
```

---

## 5) 실패 — 삭제(soft delete)된 상품

작성자가 삭제(`deleted_at`)한 상품에 `GET /api/products/{productId}/comments` 를 요청한다.

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-27T09:00:00"
}
```

## 검증 체크리스트

- [x] 비로그인 사용자도 200 + 목록 반환 (작성순)
- [x] 삭제된 댓글은 목록에서 제외
- [x] 댓글이 없으면 200 + 빈 배열 `[]`
- [x] 존재하지 않는 상품 조회 시 404 + `PRODUCT_NOT_FOUND`
- [x] 숨김 상품 조회 시 404 + `PRODUCT_NOT_FOUND`
- [x] 삭제 상품 조회 시 404 + `PRODUCT_NOT_FOUND`

## 비고

- 목록 조회에도 `productService.validateAccessibleProduct(productId)` 검증이 적용되어, **숨김·삭제 상품의 댓글 목록은 404** 로 응답한다(이슈 #80, 결정 D1). 비로그인 조회는 여전히 허용되지만 대상 상품이 접근 가능해야 한다.
- 숨김·삭제 케이스는 `CommentControllerTest`의 `getComments_hiddenProduct_returns404` / `getComments_deletedProduct_returns404`로 자동 검증한다.
- 작성자 닉네임 등 응답 확장은 MVP 범위 밖이며, 팀 회의 후 별도 PR로 진행한다.
