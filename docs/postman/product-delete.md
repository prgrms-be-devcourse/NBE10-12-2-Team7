# Product Delete Postman Scenario

> 현재 응답 형태와 예외 처리는 자동화 테스트(`ProductControllerTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## API

- Method: `DELETE`
- URL: `/api/products/{productId}`
- Auth: `Authorization: Bearer {accessToken}`
- Response: `ApiResponse<Void>`

## 사전 데이터

- 작성자 회원 1명
- 다른 회원 1명
- 카테고리 1개
- 작성자가 등록한 상품 1개

## 1. 성공 - 작성자 상품 삭제

### Request

```http
DELETE /api/products/1
Authorization: Bearer {ownerAccessToken}
```

### Expected Response

```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

### Expected DB State

- `products.deleted_at` 값이 현재 시간으로 기록된다.
- 상품 row는 물리 삭제되지 않는다.

## 2. 실패 - 인증 없음

### Request

```http
DELETE /api/products/1
```

### Expected Response

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다."
}
```

## 3. 실패 - 존재하지 않는 상품

### Request

```http
DELETE /api/products/999
Authorization: Bearer {ownerAccessToken}
```

### Expected Response

```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

## 4. 실패 - 이미 삭제된 상품 재삭제

### Request

```http
DELETE /api/products/1
Authorization: Bearer {ownerAccessToken}
```

### Expected Response

```json
{
  "status": 404,
  "error": "DELETED_PRODUCT",
  "message": "삭제된 상품입니다."
}
```

## 5. 실패 - 작성자가 아닌 사용자

### Request

```http
DELETE /api/products/1
Authorization: Bearer {otherAccessToken}
```

### Expected Response

```json
{
  "status": 403,
  "error": "PRODUCT_OWNER_ONLY",
  "message": "상품 작성자만 처리할 수 있습니다."
}
```

## 6. 정책 확인 - 거래완료 상품 삭제 허용

거래완료(`COMPLETED`) 상품도 작성자라면 삭제할 수 있다.

이 기능은 물리 삭제가 아니라 `deletedAt`만 기록하는 논리 삭제이므로 데이터와 이력은 DB에 남는다. 따라서 거래완료 상품 수정 금지 정책과 삭제 허용 정책은 충돌하지 않는다.

## Checklist

- [x] 성공 시 `200 OK` + `ApiResponse<Void>`
- [x] 작성자만 삭제 가능
- [x] 미인증 요청은 `UNAUTHORIZED`
- [x] 존재하지 않는 상품은 `PRODUCT_NOT_FOUND`
- [x] 이미 삭제된 상품은 `DELETED_PRODUCT`
- [x] 작성자가 아닌 사용자는 `PRODUCT_OWNER_ONLY`
- [x] 거래완료 상품도 작성자라면 삭제 가능
