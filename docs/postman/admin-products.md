# Postman 시나리오 — 관리자 상품 관리 (GET·PATCH·DELETE /api/admin/products)

> 아래는 **예상 응답**이다. 실제 MySQL 서버(`docker compose up -d` → `./gradlew bootRun`) 기동 후 Postman으로 각 케이스를 요청해 확인하고, `timestamp`·`productId` 등은 실제 값으로 갱신한 뒤 맨 아래 체크리스트를 체크한다.
> 관리자 계정 시드(PR 1)가 아직 없으므로, 토큰은 **수동 승격**으로 발급한다(아래 사전 조건). PR 1 머지 후에는 시드 관리자 계정으로 바로 로그인하면 된다.

## 공통
- Method: `GET`
- URL: `/api/admin/products`, `/api/admin/products/{productId}`
- 인증: **ROLE_ADMIN 필요** (`Authorization: Bearer {adminAccessToken}`)
  - 미인증(토큰 없음/오류) → 401 `UNAUTHORIZED`
  - 인증됐으나 일반 사용자(ROLE_USER) → 403 `FORBIDDEN`
- 목록은 **숨김(hidden)·삭제(deletedAt) 여부와 무관하게 전체 상품**을 반환한다(일반 목록과 다른 점).
- 정렬은 별도 지정하지 않았다(기본 `id` 순). 정렬 요구가 생기면 후속 작업으로 추가한다.

### 사전 조건 1 — 관리자 토큰 발급 (시드 전까지 수동)
```
1. 회원 가입:  POST /api/auth/signup   { "email":"admin@example.com", "password":"password123", "nickname":"adminUser" }
2. 관리자 승격(MySQL): UPDATE members SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';
3. 로그인:    POST /api/auth/login     { "email":"admin@example.com", "password":"password123" } → data.accessToken 복사
   ⚠ 반드시 승격(2) 후 로그인(3). 권한은 로그인 시점에 토큰에 박힌다.
```

### 사전 조건 2 — 검증용 상품 준비
일반 사용자로 상품을 몇 개 등록한 뒤, 숨김/삭제 상태를 섞어 두면 "관리자는 전체를 본다"를 확인할 수 있다.
```
1. 판매자 가입·로그인 → 토큰
2. GET /api/categories 로 categoryId 확인
3. POST /api/products 로 상품 2~3개 등록
4. (선택) 숨김 상품 만들기:  UPDATE products SET hidden = true WHERE id = {productId};
5. (선택) 삭제 상품 만들기:  DELETE /api/products/{productId}  (작성자 본인 토큰으로 — 소프트 삭제)
```

---

# GET /api/admin/products — 상품 목록 조회

## 1) 성공 — 관리자 토큰 (숨김·삭제 포함 전체)

**Request**
```
GET /api/admin/products
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "productId": 1,
      "memberId": 2,
      "categoryId": 1,
      "title": "아이폰 15",
      "description": "상태 좋은 아이폰입니다.",
      "price": 800000,
      "tradeStatus": "ON_SALE",
      "region": "서울 강남구",
      "viewCount": 3,
      "hidden": false,
      "deletedAt": null,
      "createdAt": "2026-06-28T10:00:00"
    },
    {
      "productId": 2,
      "memberId": 2,
      "categoryId": 1,
      "title": "숨김 상품",
      "description": "숨김 처리됨",
      "price": 5000,
      "tradeStatus": "ON_SALE",
      "region": "서울 서초구",
      "viewCount": 0,
      "hidden": true,
      "deletedAt": null,
      "createdAt": "2026-06-28T10:01:00"
    },
    {
      "productId": 3,
      "memberId": 2,
      "categoryId": 1,
      "title": "삭제된 상품",
      "description": "작성자가 삭제함",
      "price": 12000,
      "tradeStatus": "ON_SALE",
      "region": "서울 송파구",
      "viewCount": 1,
      "hidden": false,
      "deletedAt": "2026-06-28T11:00:00",
      "createdAt": "2026-06-28T10:02:00"
    }
  ]
}
```
> 일반 목록과 달리 숨김(`productId: 2`)·삭제(`productId: 3`) 상품도 포함된다.

---

## 2) 실패 — 일반 사용자(ROLE_USER) 접근

**Request**
```
GET /api/admin/products
Authorization: Bearer {userAccessToken}
```

**Response** `403 Forbidden`
```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "접근 권한이 없습니다.",
  "timestamp": "2026-06-28T10:05:00.000000"
}
```

---

## 3) 실패 — 토큰 없음

**Request**
```
GET /api/admin/products
(Authorization 헤더 없음)
```

**Response** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2026-06-28T10:06:00.000000"
}
```

---

# GET /api/admin/products/{productId} — 상품 상세 조회

## 1) 성공 — 관리자 토큰

**Request**
```
GET /api/admin/products/1
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "productId": 1,
    "memberId": 2,
    "categoryId": 1,
    "title": "아이폰 15",
    "description": "상태 좋은 아이폰입니다.",
    "price": 800000,
    "tradeStatus": "ON_SALE",
    "region": "서울 강남구",
    "viewCount": 3,
    "hidden": false,
    "deletedAt": null,
    "createdAt": "2026-06-28T10:00:00"
  }
}
```

---

## 2) 실패 — 존재하지 않는 상품

**Request**
```
GET /api/admin/products/999999
Authorization: Bearer {adminAccessToken}
```

**Response** `404 Not Found`
```json
{
  "status": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-06-28T10:07:00.000000"
}
```

---

## 3) 실패 — 권한/인증 (목록과 동일)
- 일반 사용자 토큰 → `403 FORBIDDEN`
- 토큰 없음 → `401 UNAUTHORIZED`

---

# PATCH /api/admin/products/{productId}/hidden — 상품 숨김

> 관리자가 작성자가 아니어도 상품을 숨김 처리한다. **한방향(숨김=true)** 이며, un-hide(해제)는 현재 미지원(`Product.changeHidden` 메서드 필요 — 후속 작업).

## 1) 성공 — 관리자

**Request**
```
PATCH /api/admin/products/1/hidden
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{ "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
```
> 이후 상세 조회 시 `hidden: true`로 보인다.

## 2) 실패 — 존재하지 않는 상품
**Response** `404 Not Found`
```json
{ "status": 404, "error": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다.", "timestamp": "2026-06-28T10:07:00.000000" }
```

## 3) 실패 — 권한/인증
- 일반 사용자 토큰 → `403 FORBIDDEN` / 토큰 없음 → `401 UNAUTHORIZED`

---

# DELETE /api/admin/products/{productId} — 상품 소프트 삭제

## 1) 성공 — 관리자

**Request**
```
DELETE /api/admin/products/1
Authorization: Bearer {adminAccessToken}
```

**Response** `200 OK`
```json
{ "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
```
> 소프트 삭제이므로 `deleted_at`만 기록되고, 이후 상세 조회 시 `deletedAt != null`로 보인다.

## 2) 실패 — 존재하지 않는 상품
**Response** `404 Not Found`
```json
{ "status": 404, "error": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다.", "timestamp": "2026-06-28T10:07:00.000000" }
```

## 3) 실패 — 권한/인증
- 일반 사용자 토큰 → `403 FORBIDDEN` / 토큰 없음 → `401 UNAUTHORIZED`

---

## 검증 체크리스트

### GET /api/admin/products
- [ ] 관리자 성공 시 200 + `data` 배열(숨김·삭제 포함 전체)
- [ ] 각 원소에 productId/memberId/categoryId/title/description/price/tradeStatus/region/viewCount/hidden/deletedAt/createdAt 포함
- [ ] 숨김 상품(`hidden: true`)·삭제 상품(`deletedAt != null`)도 포함됨
- [ ] 일반 사용자 → 403 + `FORBIDDEN`(COMMON_004)
- [ ] 토큰 없음 → 401 + `UNAUTHORIZED`(COMMON_003)

### GET /api/admin/products/{productId}
- [ ] 관리자 성공 시 200 + 해당 상품 정보
- [ ] 없는 상품 → 404 + `PRODUCT_NOT_FOUND`(PRODUCT_001)
- [ ] 일반 사용자 → 403 + `FORBIDDEN`(COMMON_004)
- [ ] 토큰 없음 → 401 + `UNAUTHORIZED`(COMMON_003)

### PATCH /api/admin/products/{productId}/hidden
- [ ] 관리자 성공 200 + 이후 상세 조회 시 `hidden: true`
- [ ] 없는 상품 → 404 + `PRODUCT_NOT_FOUND`(PRODUCT_001)
- [ ] 일반 사용자 → 403 `FORBIDDEN` / 토큰 없음 → 401 `UNAUTHORIZED`

### DELETE /api/admin/products/{productId}
- [ ] 관리자 성공 200 + 소프트 삭제(`deletedAt` 기록)
- [ ] 없는 상품 → 404 + `PRODUCT_NOT_FOUND`(PRODUCT_001)
- [ ] 일반 사용자 → 403 `FORBIDDEN` / 토큰 없음 → 401 `UNAUTHORIZED`
