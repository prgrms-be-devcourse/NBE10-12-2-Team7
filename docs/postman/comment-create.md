# Postman 시나리오 — 댓글 작성 (POST /api/products/{productId}/comments)

> 현재 응답 형태와 예외 처리는 자동화 테스트(`CommentControllerTest`, `CommentServiceTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `POST`
- URL: `/api/products/{productId}/comments`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {accessToken}`
- 인증 필요 (로그인 사용자만)
- 사전 조건:
  - 회원가입/로그인으로 Access Token을 발급받는다.
  - 댓글을 작성할 대상 상품(`productId`)이 존재해야 한다.

---

## 1) 성공 — 댓글 작성

**Request Body**
```json
{
  "content": "좋은 상품이네요"
}
```

**Response** `201 Created`
```json
{
  "status": 201,
  "message": "댓글이 작성되었습니다.",
  "data": {
    "id": 1,
    "memberId": 1,
    "productId": 1,
    "content": "좋은 상품이네요",
    "createdAt": "2026-06-26T09:00:00",
    "updatedAt": "2026-06-26T09:00:00"
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

## 3) 실패 — 존재하지 않는 상품

`POST /api/products/999999/comments`

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 4) 실패 — 내용 공백

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

---

## 5) 실패 — 숨김(hidden) 처리된 상품

관리자가 숨김 처리한 상품에 `POST /api/products/{productId}/comments` 를 요청한다.

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 6) 실패 — 삭제(soft delete)된 상품

작성자가 삭제(`deleted_at`)한 상품에 `POST /api/products/{productId}/comments` 를 요청한다.

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

## 검증 체크리스트

- [x] 인증 없음 시 401 + `UNAUTHORIZED`
- [x] 성공 시 201 + `ApiResponse<CommentResponse>`
- [x] 존재하지 않는 상품 시 404 + `PRODUCT_NOT_FOUND`
- [x] 내용 공백 시 400 + `INVALID_INPUT_VALUE`
- [x] 내용 500자 초과 시 400 + `INVALID_INPUT_VALUE` (`@Size(max = 500)`)
- [x] 숨김 상품 시 404 + `PRODUCT_NOT_FOUND`
- [x] 삭제 상품 시 404 + `PRODUCT_NOT_FOUND`

## 비고

- 상품 존재 검증은 `productService.validateAccessibleProduct(productId)`(내부적으로 `existsByIdAndDeletedAtIsNullAndHiddenFalse`)로 중앙화되어, **소프트삭제(`deleted_at`)·숨김(`hidden`) 상품은 댓글 작성이 차단(404)** 된다. (기존 `existsById`는 삭제·숨김도 통과하던 갭을 해소함 — 이슈 #80 리팩터, Favorite 등록과 동일 사안.)
- 숨김·삭제 케이스는 `CommentControllerTest`의 `createComment_hiddenProduct_returns404`로 자동 검증한다.
