# Postman 시나리오 — 내 관심 상품 목록 조회 (GET /api/members/me/favorites)

> 현재 응답 형태와 예외 처리는 자동화 테스트(`FavoriteControllerTest`, `FavoriteServiceTest`)로 검증했다. 실제 MySQL 서버 기동 후 Postman으로도 같은 시나리오를 확인한다.

## 공통

- Method: `GET`
- URL: `/api/members/me/favorites`
- Headers:
  - `Authorization: Bearer {accessToken}`
- 인증 필요 (본인 관심 목록만 조회)
- 정렬: 최근 등록순 (`created_at DESC`)
- 사전 조건:
  - 회원가입/로그인으로 Access Token을 발급받는다.
  - 목록을 확인하려면 관심 상품을 미리 등록해 둔다. (`POST /api/products/{productId}/favorites`)

---

## 1) 성공 — 내 관심 목록 조회

관심 상품을 2개(상품 1 → 상품 2 순) 등록한 뒤 `GET /api/members/me/favorites`

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 2,
      "productId": 2,
      "createdAt": "2026-06-28T09:01:00"
    },
    {
      "id": 1,
      "productId": 1,
      "createdAt": "2026-06-28T09:00:00"
    }
  ]
}
```
> 최근 등록한 상품(상품 2)이 목록 맨 앞에 온다.

---

## 2) 성공 — 관심 상품이 없는 경우 (빈 목록)

관심 등록을 하나도 하지 않은 사용자가 `GET /api/members/me/favorites`

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": []
}
```

---

## 3) 실패 — 인증 없음

`Authorization` 헤더 없이 요청한다.

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-28T09:00:00"
}
```

## 검증 체크리스트

- [x] 본인 관심 목록 200 + 최근 등록순(`created_at DESC`) 반환
- [x] 관심 상품이 없으면 200 + 빈 배열 `[]`
- [x] 인증 없음 시 401 + `UNAUTHORIZED`

## 비고

- 상품 제목/가격/거래상태 등 요약 정보와 삭제·숨김 상품 필터 정책은 MVP 범위 밖이며, 팀 회의 후 별도 PR로 진행한다.
