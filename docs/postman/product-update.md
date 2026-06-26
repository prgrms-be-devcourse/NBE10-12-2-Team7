# Postman 시나리오 — 상품 수정 (PATCH /api/products/{productId})

> 현재 응답 형태와 예외 처리는 자동화 테스트(`ProductControllerTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `PATCH`
- URL: `/api/products/{productId}`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {accessToken}`
- 인증 필요
- 작성자 본인만 수정 가능
- 숨김 상품은 작성자가 수정 가능
- 거래완료(`COMPLETED`) 상품은 수정 불가

---

## 1) 성공 — 상품 수정

**Request Body**
```json
{
  "categoryId": 2,
  "title": "맥북 프로",
  "description": "수정된 상품 설명입니다.",
  "price": 1500000,
  "region": "서울 서초구"
}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "productId": 1,
    "memberId": 1,
    "categoryId": 2,
    "title": "맥북 프로",
    "description": "수정된 상품 설명입니다.",
    "price": 1500000,
    "tradeStatus": "ON_SALE",
    "region": "서울 서초구",
    "viewCount": 0,
    "hidden": false
  }
}
```

---

## 2) 실패 — 인증 없음

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

---

## 3) 실패 — 작성자 아님

**Response** `403 Forbidden`
```json
{
  "status": 403,
  "error": "PRODUCT_OWNER_ONLY",
  "message": "상품 작성자만 처리할 수 있습니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

---

## 4) 실패 — 거래완료 상품 수정

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "CANNOT_UPDATE_COMPLETED_PRODUCT",
  "message": "거래완료된 상품은 수정할 수 없습니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

---

## 5) 실패 — 존재하지 않는 카테고리

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "CATEGORY_NOT_FOUND",
  "message": "카테고리를 찾을 수 없습니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

---

## 6) 실패 — 제목 공백

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_PRODUCT_TITLE",
  "message": "상품 제목은 필수입니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

---

## 7) 실패 — 가격 음수

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_PRODUCT_PRICE",
  "message": "상품 가격은 0원 이상이어야 합니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

## 검증 체크리스트

- [x] 인증 없음 시 401 + `UNAUTHORIZED`
- [x] 성공 시 200 + `ApiResponse<ProductResponse>`
- [x] 작성자 아님 시 403 + `PRODUCT_OWNER_ONLY`
- [x] 거래완료 상품 수정 시 400 + `CANNOT_UPDATE_COMPLETED_PRODUCT`
- [x] 카테고리 없음 시 404 + `CATEGORY_NOT_FOUND`
- [x] 제목 공백 시 400 + `INVALID_PRODUCT_TITLE`
- [x] 가격 음수 시 400 + `INVALID_PRODUCT_PRICE`
