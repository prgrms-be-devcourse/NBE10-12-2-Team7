# Postman 시나리오 — 관심 상품 등록 (POST /api/products/{productId}/favorites)

> 현재 응답 형태와 예외 처리는 자동화 테스트(`FavoriteControllerTest`, `FavoriteServiceTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `POST`
- URL: `/api/products/{productId}/favorites`
- Headers:
  - `Authorization: Bearer {accessToken}`
- 인증 필요 (로그인 사용자만)
- 사전 조건:
  - 회원가입/로그인으로 Access Token을 발급받는다.
  - 관심 등록할 대상 상품(`productId`)이 존재해야 한다.

---

## 1) 성공 — 관심 상품 등록

`POST /api/products/1/favorites` (Body 없음)

**Response** `201 Created`
```json
{
  "status": 201,
  "message": "관심 상품으로 등록되었습니다.",
  "data": {
    "id": 1,
    "productId": 1,
    "createdAt": "2026-06-26T09:00:00"
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

`POST /api/products/999999/favorites`

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

## 4) 실패 — 중복 관심 등록

이미 관심 등록한 상품에 다시 `POST /api/products/1/favorites` 를 요청한다.

**Response** `409 Conflict`
```json
{
  "status": 409,
  "error": "FAVORITE_ALREADY_EXISTS",
  "message": "이미 관심 등록한 상품입니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 5) 실패 — 숨김(hidden) 처리된 상품

관리자가 숨김 처리한 상품에 `POST /api/products/{productId}/favorites` 를 요청한다.

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

> 숨김 상품은 존재 자체를 노출하지 않기 위해 `403 HIDDEN_PRODUCT`가 아닌 `404 PRODUCT_NOT_FOUND`로 통일한다.

---

## 6) 실패 — 삭제(soft delete)된 상품

작성자가 삭제(`deleted_at`)한 상품에 `POST /api/products/{productId}/favorites` 를 요청한다.

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
- [x] 성공 시 201 + `ApiResponse<FavoriteResponse>`
- [x] 존재하지 않는 상품 시 404 + `PRODUCT_NOT_FOUND`
- [x] 중복 관심 등록 시 409 + `FAVORITE_ALREADY_EXISTS`
- [x] 숨김 상품 시 404 + `PRODUCT_NOT_FOUND`
- [x] 삭제 상품 시 404 + `PRODUCT_NOT_FOUND`

## 비고

- 상품 존재 검증은 `productService.validateAccessibleProduct(productId)`(내부적으로 `existsByIdAndDeletedAtIsNullAndHiddenFalse`)로 중앙화되어, **소프트삭제(`deleted_at`)·숨김(`hidden`) 상품은 관심 등록이 차단(404)** 된다. (기존 `existsById`는 삭제·숨김도 통과하던 갭을 해소함 — 이슈 #80 리팩터.)
- 숨김·삭제 케이스는 `FavoriteControllerTest`의 `addFavorite_hiddenProduct_returns404` / `addFavorite_deletedProduct_returns404`로 자동 검증한다.
