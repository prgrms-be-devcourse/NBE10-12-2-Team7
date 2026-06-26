# Postman 시나리오 — 관심 상품 취소 (DELETE /api/products/{productId}/favorites)

> 현재 응답 형태와 예외 처리는 자동화 테스트(`FavoriteControllerTest`, `FavoriteServiceTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `DELETE`
- URL: `/api/products/{productId}/favorites`
- Headers:
  - `Authorization: Bearer {accessToken}`
- 인증 필요 (로그인 사용자만)
- 사전 조건:
  - 회원가입/로그인으로 Access Token을 발급받는다.
  - 취소할 관심 상품이 미리 등록되어 있어야 한다. (`POST /api/products/{productId}/favorites`)

---

## 1) 성공 — 관심 상품 취소

이미 관심 등록한 상품에 `DELETE /api/products/1/favorites` (Body 없음)

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "관심 상품에서 제거되었습니다.",
  "data": null
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

## 3) 실패 — 등록하지 않은 관심 취소

관심 등록한 적 없는 상품에 `DELETE /api/products/1/favorites`

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "FAVORITE_NOT_FOUND",
  "message": "관심 상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-26T09:00:00"
}
```

## 검증 체크리스트

- [x] 인증 없음 시 401 + `UNAUTHORIZED`
- [x] 등록한 관심 취소 시 200 (data 없음)
- [x] 등록하지 않은 관심 취소 시 404 + `FAVORITE_NOT_FOUND`

## 비고

- 관심 취소는 `(member_id, product_id)`로 본인 관심만 조회·삭제하므로 별도 권한 검증이 필요 없다 (본인 것만 매칭됨).
- 본인 관심을 제거하는 동작이라 상품 존재/숨김/삭제 상태와 무관하다. (상품 visibility 검증 불필요)
