# Category Products Postman Scenario

> 현재 응답 형태와 예외 처리는 자동화 테스트(`CategoryControllerTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## API

- Method: `GET`
- URL: `/api/categories/{categoryId}/products`
- Auth: 불필요
- Response: `ApiResponse<List<ProductSummaryResponse>>`

## 정책

- 삭제되지 않은 상품만 조회한다.
- 숨김 처리되지 않은 상품만 조회한다.
- 최신 등록순으로 조회한다.
- 카테고리가 존재하지 않으면 `CATEGORY_NOT_FOUND`를 반환한다.
- 카테고리는 존재하지만 상품이 없으면 `200 OK`와 빈 배열을 반환한다.

## 1. 성공 - 카테고리별 상품 목록 조회

### Request

```http
GET /api/categories/1/products
```

### Expected Response

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

## 2. 성공 - 상품 없는 카테고리

### Request

```http
GET /api/categories/8/products
```

### Expected Response

```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": []
}
```

## 3. 실패 - 존재하지 않는 카테고리

### Request

```http
GET /api/categories/9999/products
```

### Expected Response

```json
{
  "status": 404,
  "error": "CATEGORY_NOT_FOUND",
  "message": "카테고리를 찾을 수 없습니다."
}
```

## 4. 정책 확인 - 삭제·숨김 상품 제외

같은 카테고리에 속하더라도 아래 상품은 응답 목록에서 제외한다.

- `deletedAt`이 `null`이 아닌 상품
- `hidden`이 `true`인 상품

## Checklist

- [x] 인증 없이 조회 가능
- [x] 성공 시 `200 OK` + `ApiResponse<List<ProductSummaryResponse>>`
- [x] Entity 직접 반환 없음
- [x] 삭제 상품 제외
- [x] 숨김 상품 제외
- [x] 최신 등록순
- [x] 존재하지 않는 카테고리는 `CATEGORY_NOT_FOUND`
- [x] 상품 없는 카테고리는 빈 배열
