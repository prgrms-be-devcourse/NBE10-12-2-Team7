# Product My List Postman Scenario

> 현재 응답 형태와 조회 정책은 자동화 테스트(`ProductControllerTest`, `ProductRepositoryTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## API

- Method: `GET`
- URL: `/api/products/me`
- Auth: `Authorization: Bearer {accessToken}`
- Response: `ApiResponse<List<ProductSummaryResponse>>`

## 정책

- 로그인한 사용자의 상품만 조회한다.
- 삭제된 상품은 제외한다.
- 숨김 상품은 포함한다.
- 최신 등록순으로 조회한다.
- 상품이 없으면 빈 배열을 반환한다.

## 1. 성공 - 내 상품 목록 조회

### Request

```http
GET /api/products/me
Authorization: Bearer {accessToken}
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
      "title": "숨김 내 상품",
      "price": 20000,
      "tradeStatus": "ON_SALE",
      "region": "서울 서초구",
      "viewCount": 0,
      "hidden": true
    },
    {
      "productId": 1,
      "memberId": 1,
      "categoryId": 1,
      "title": "오래된 내 상품",
      "price": 10000,
      "tradeStatus": "ON_SALE",
      "region": "서울 강남구",
      "viewCount": 0,
      "hidden": false
    }
  ]
}
```

## 2. 성공 - 내 상품 없음

### Request

```http
GET /api/products/me
Authorization: Bearer {accessToken}
```

### Expected Response

```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": []
}
```

## 3. 실패 - 인증 없음

### Request

```http
GET /api/products/me
```

### Expected Response

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다."
}
```

## 검증 체크리스트

- [x] 인증 필요
- [x] 본인 상품만 조회
- [x] 삭제 상품 제외
- [x] 숨김 상품 포함
- [x] 최신 등록순
- [x] 상품 없음 시 빈 배열
