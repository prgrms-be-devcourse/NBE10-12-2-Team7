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

## 검증 체크리스트

- [x] 인증 없음 시 401 + `UNAUTHORIZED`
- [x] 성공 시 201 + `ApiResponse<FavoriteResponse>`
- [x] 존재하지 않는 상품 시 404 + `PRODUCT_NOT_FOUND`
- [x] 중복 관심 등록 시 409 + `FAVORITE_ALREADY_EXISTS`

## 비고

- 상품 존재 검증은 `productRepository.existsById(productId)`로 처리한다. 현재는 소프트삭제(`deleted_at`)·숨김(`hidden`) 상품도 존재로 간주하므로, "삭제/숨김 상품 관심 등록 차단"이 필요하면 Product 도메인에 필터 조회 메서드 추가를 협의한다.
