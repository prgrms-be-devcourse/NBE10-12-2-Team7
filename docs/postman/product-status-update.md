# Product Status Update Postman Scenario

> 현재 응답 형태와 예외 처리는 자동화 테스트(`ProductControllerTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## API

- Method: `PATCH`
- URL: `/api/products/{productId}/status`
- Headers:
  - `Content-Type: application/json`
  - `Authorization: Bearer {accessToken}`
- Response: `ApiResponse<ProductResponse>`

## 사전 데이터

- 작성자 회원 1명
- 다른 회원 1명
- 카테고리 1개
- 작성자가 등록한 상품 1개

## 1. 성공 - 작성자 상품 거래 상태 변경

### Request

```http
PATCH /api/products/1/status
Authorization: Bearer {ownerAccessToken}
Content-Type: application/json
```

```json
{
  "tradeStatus": "RESERVED"
}
```

### Expected Response

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
    "tradeStatus": "RESERVED",
    "region": "서울 강남구",
    "viewCount": 0,
    "hidden": false
  }
}
```

## 2. 성공 - 같은 거래 상태 요청

같은 상태로 변경 요청하면 예외 없이 현재 상태를 그대로 반환한다.

```json
{
  "tradeStatus": "ON_SALE"
}
```

## 3. 성공 - 거래완료 상태 유지

거래완료(`COMPLETED`) 상품에 다시 `COMPLETED`를 요청하면 예외 없이 현재 상태를 그대로 반환한다.

```json
{
  "tradeStatus": "COMPLETED"
}
```

## 4. 성공 - 숨김 상품 상태 변경

숨김 상품도 작성자라면 상태를 변경할 수 있다.

```json
{
  "tradeStatus": "RESERVED"
}
```

## 5. 실패 - 인증 없음

### Request

```http
PATCH /api/products/1/status
Content-Type: application/json
```

### Expected Response

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다."
}
```

## 6. 실패 - 존재하지 않는 상품

### Expected Response

```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

## 7. 실패 - 삭제된 상품

### Expected Response

```json
{
  "status": 404,
  "error": "DELETED_PRODUCT",
  "message": "삭제된 상품입니다."
}
```

## 8. 실패 - 작성자가 아닌 사용자

### Request

```http
PATCH /api/products/1/status
Authorization: Bearer {otherAccessToken}
Content-Type: application/json
```

```json
{
  "tradeStatus": "RESERVED"
}
```

### Expected Response

```json
{
  "status": 403,
  "error": "PRODUCT_OWNER_ONLY",
  "message": "상품 작성자만 처리할 수 있습니다."
}
```

## 9. 실패 - 거래완료 상품을 다른 상태로 변경

거래완료(`COMPLETED`) 상품은 `ON_SALE`, `RESERVED`로 되돌릴 수 없다.

### Request

```json
{
  "tradeStatus": "ON_SALE"
}
```

### Expected Response

```json
{
  "status": 400,
  "error": "CANNOT_CHANGE_COMPLETED_PRODUCT",
  "message": "거래완료된 상품은 상태를 변경할 수 없습니다."
}
```

## 10. 실패 - 유효하지 않은 거래 상태

### Request

```json
{
  "tradeStatus": "INVALID"
}
```

### Expected Response

```json
{
  "status": 400,
  "error": "INVALID_TRADE_STATUS",
  "message": "잘못된 거래 상태입니다."
}
```

## 11. 실패 - 비어 있는 거래 상태

### Request

```json
{
  "tradeStatus": " "
}
```

### Expected Response

```json
{
  "status": 400,
  "error": "INVALID_TRADE_STATUS",
  "message": "잘못된 거래 상태입니다."
}
```

## 12. 실패 - null 거래 상태

### Request

```json
{
  "tradeStatus": null
}
```

### Expected Response

```json
{
  "status": 400,
  "error": "INVALID_TRADE_STATUS",
  "message": "잘못된 거래 상태입니다."
}
```

## 13. 실패 - 거래 상태 필드 누락

### Request

```json
{
}
```

### Expected Response

```json
{
  "status": 400,
  "error": "INVALID_TRADE_STATUS",
  "message": "잘못된 거래 상태입니다."
}
```

## 정책 확인

- 상태 값은 `ON_SALE`, `RESERVED`, `COMPLETED`만 허용한다.
- 작성자만 상태를 변경할 수 있다.
- 삭제된 상품은 상태 변경이 불가능하다.
- 숨김 상품은 작성자라면 상태 변경할 수 있다.
- 거래완료 상품은 다른 상태로 되돌릴 수 없다.

## Checklist

- [x] 성공 시 `200 OK` + `ApiResponse<ProductResponse>`
- [x] 인증 없음 시 `UNAUTHORIZED`
- [x] 상품 없음 시 `PRODUCT_NOT_FOUND`
- [x] 삭제된 상품은 `DELETED_PRODUCT`
- [x] 작성자가 아닌 사용자는 `PRODUCT_OWNER_ONLY`
- [x] 거래완료 상품 되돌리기는 `CANNOT_CHANGE_COMPLETED_PRODUCT`
- [x] 잘못된 거래 상태는 `INVALID_TRADE_STATUS`
- [x] 비어 있는 거래 상태는 `INVALID_TRADE_STATUS`
- [x] null 거래 상태는 `INVALID_TRADE_STATUS`
- [x] 거래 상태 필드 누락은 `INVALID_TRADE_STATUS`
