# Postman 시나리오 — 상품 상세 조회 (GET /api/products/{productId})

> 현재 응답 형태와 예외 처리는 자동화 테스트(`ProductControllerTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `GET`
- URL: `/api/products/{productId}`
- 인증 불필요
- 성공 시 조회수 `viewCount`가 1 증가한다.

---

## 1) 성공 — 상품 상세 조회

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "productId": 1,
    "memberId": 1,
    "categoryId": 1,
    "title": "아이폰 15",
    "description": "상태 좋은 아이폰입니다.",
    "price": 800000,
    "tradeStatus": "ON_SALE",
    "region": "서울 강남구",
    "viewCount": 1,
    "hidden": false
  }
}
```

---

## 2) 실패 — 존재하지 않는 상품

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

---

## 3) 실패 — 삭제된 상품

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "DELETED_PRODUCT",
  "message": "삭제된 상품입니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

---

## 4) 실패 — 숨김 상품

**Response** `403 Forbidden`
```json
{
  "status": 403,
  "error": "HIDDEN_PRODUCT",
  "message": "숨김 처리된 상품입니다.",
  "timestamp": "2026-06-26T12:00:00"
}
```

## 검증 체크리스트

- [x] 인증 없이 상세 조회 가능
- [x] 성공 시 `200 OK` + `ApiResponse<ProductResponse>`
- [x] 성공 시 `description` 포함
- [x] 성공 시 `viewCount` 1 증가
- [x] 존재하지 않는 상품 시 404 + `PRODUCT_NOT_FOUND`
- [x] 삭제 상품 시 404 + `DELETED_PRODUCT`
- [x] 숨김 상품 시 403 + `HIDDEN_PRODUCT`
