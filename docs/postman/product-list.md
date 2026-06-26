# Postman 시나리오 — 상품 목록 조회 (GET /api/products)

> 현재 응답 형태와 필터링 조건은 자동화 테스트(`ProductControllerTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `GET`
- URL: `/api/products`
- 인증 불필요
- 정렬: 최신 등록순(`id` 내림차순)
- 조회 제외:
  - `deletedAt`이 있는 상품
  - `hidden=true` 상품

---

## 1) 성공 — 상품 목록 조회

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "productId": 2,
      "memberId": 1,
      "categoryId": 1,
      "title": "최신 상품",
      "price": 20000,
      "tradeStatus": "ON_SALE",
      "region": "서울 서초구",
      "viewCount": 0,
      "hidden": false
    },
    {
      "productId": 1,
      "memberId": 1,
      "categoryId": 1,
      "title": "오래된 상품",
      "price": 10000,
      "tradeStatus": "ON_SALE",
      "region": "서울 강남구",
      "viewCount": 0,
      "hidden": false
    }
  ]
}
```

---

## 2) 성공 — 조회 가능한 상품 없음

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": []
}
```

## 검증 체크리스트

- [x] 인증 없이 조회 가능
- [x] `200 OK` + `ApiResponse<List<ProductSummaryResponse>>`
- [x] 최신 등록순으로 반환
- [x] 삭제 상품 제외
- [x] 숨김 상품 제외
- [x] 목록 응답에서 `description` 제외
