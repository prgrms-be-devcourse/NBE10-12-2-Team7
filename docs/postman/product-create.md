# Postman 시나리오 — 상품 등록 (POST /api/products)

> 현재 응답 형태와 예외 처리는 자동화 테스트(`ProductControllerTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `POST`
- URL: `/api/products`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {accessToken}`
- 인증 필요
- 사전 조건:
  - 회원가입/로그인으로 Access Token을 발급받는다.
  - `GET /api/categories`로 존재하는 `categoryId`를 확인한다.

---

## 1) 성공 — 상품 등록

**Request Body**
```json
{
  "categoryId": 1,
  "title": "아이폰 15",
  "description": "상태 좋은 아이폰입니다.",
  "price": 800000,
  "region": "서울 강남구"
}
```

**Response** `201 Created`
```json
{
  "status": 201,
  "message": "상품이 등록되었습니다.",
  "data": {
    "productId": 1,
    "memberId": 1,
    "categoryId": 1,
    "title": "아이폰 15",
    "description": "상태 좋은 아이폰입니다.",
    "price": 800000,
    "tradeStatus": "ON_SALE",
    "region": "서울 강남구",
    "viewCount": 0,
    "hidden": false
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

## 3) 실패 — 존재하지 않는 카테고리

**Request Body**
```json
{
  "categoryId": 999,
  "title": "아이폰 15",
  "description": "상태 좋은 아이폰입니다.",
  "price": 800000,
  "region": "서울 강남구"
}
```

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "CATEGORY_NOT_FOUND",
  "message": "카테고리를 찾을 수 없습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 4) 실패 — 제목 공백

**Request Body**
```json
{
  "categoryId": 1,
  "title": " ",
  "description": "상태 좋은 아이폰입니다.",
  "price": 800000,
  "region": "서울 강남구"
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_PRODUCT_TITLE",
  "message": "상품 제목은 필수입니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

---

## 5) 실패 — 가격 음수

**Request Body**
```json
{
  "categoryId": 1,
  "title": "아이폰 15",
  "description": "상태 좋은 아이폰입니다.",
  "price": -1,
  "region": "서울 강남구"
}
```

**Response** `400 Bad Request`
```json
{
  "status": 400,
  "error": "INVALID_PRODUCT_PRICE",
  "message": "상품 가격은 0원 이상이어야 합니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

## 검증 체크리스트

- [x] 인증 없음 시 401 + `UNAUTHORIZED`
- [x] 성공 시 201 + `ApiResponse<ProductResponse>`
- [x] 성공 시 기본값 `tradeStatus=ON_SALE`, `viewCount=0`, `hidden=false`
- [x] 존재하지 않는 카테고리 시 404 + `CATEGORY_NOT_FOUND`
- [x] 제목 공백 시 400 + `INVALID_PRODUCT_TITLE`
- [x] 가격 음수 시 400 + `INVALID_PRODUCT_PRICE`
