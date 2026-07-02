# 03. Product (상품 CRUD·검색·상태) — 담당: 한상민

대상: `ProductService`, `ProductController`, `MyProductController`, `ProductRepository`, `ProductSpecification`
관련 ErrorCode: `PRODUCT_NOT_FOUND`, `PRODUCT_OWNER_ONLY`, `HIDDEN_PRODUCT`, `DELETED_PRODUCT`, `INVALID_PRODUCT_TITLE`, `INVALID_PRODUCT_PRICE`, `CANNOT_UPDATE_COMPLETED_PRODUCT`, `CANNOT_CHANGE_COMPLETED_PRODUCT`, `INVALID_TRADE_STATUS`, `INVALID_SEARCH_CONDITION`, `CATEGORY_NOT_FOUND`, `MEMBER_NOT_FOUND`

> ⚠️ Product 요청 DTO에는 **`@Valid`가 없음** → title/price 검증은 **서비스(S)**. (전략 §4)
> ⚠️ 검증/소유자/상태 검사에는 **순서**가 있음 — 아래 "검증 순서" 표가 핵심.

## 핵심 로직 요약

- 생성: `validateProductFields`(title 공백·price null/음수) → 회원 조회 → 카테고리 조회 → `Product.create`(ON_SALE, viewCount 0, hidden false).
- 목록/카테고리별/검색: **`deletedAt IS NULL AND hidden=false`** 필터, **id DESC** 정렬.
- 내 상품: `findAllByMemberIdAndDeletedAtIsNull` — **hidden 포함**, 삭제만 제외.
- 상세(getProduct): **`@Transactional`** — not found → deleted → hidden 순 검사 후 **viewCount +1**.
- 수정/삭제/상태: 검사 순서 = (검증) → not found → **deleted → owner → (completed/status규칙)**.

### 검증 순서 (회귀 방지 핵심)

| 작업 | 순서 |
|---|---|
| createProduct | ① title/price 검증 → ② 회원 → ③ 카테고리 |
| getProduct | ① not found → ② deleted → ③ hidden → ④ viewCount++ |
| updateProduct | ① title/price 검증 → ② not found → ③ deleted → ④ owner → ⑤ completed → ⑥ 카테고리 |
| deleteProduct | ① not found → ② deleted → ③ owner |
| updateProductStatus | ① tradeStatus 파싱 → ② not found → ③ deleted → ④ owner → ⑤ completed 규칙 |

## 상품 등록 (POST /api/products) — 인증 필요

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-01 | S | 정상 입력 | 저장, `tradeStatus=ON_SALE`, `viewCount=0`, `hidden=false` |
| PR-02 | S | title 공백("", " ", null) | `INVALID_PRODUCT_TITLE` (400) |
| PR-03 | S | price = null | `INVALID_PRODUCT_PRICE` (400) |
| PR-04 | S | price < 0 | `INVALID_PRODUCT_PRICE` |
| PR-05 | S | ⭐ price = 0 | **허용**(0원 이상) |
| PR-06 | S | 회원 id로 회원 없음 | `MEMBER_NOT_FOUND` |
| PR-07 | S | categoryId로 카테고리 없음 | `CATEGORY_NOT_FOUND` |
| PR-08 | S | ⭐ title 공백 **&** 회원도 없음 | `INVALID_PRODUCT_TITLE` (검증이 회원조회보다 먼저) |
| PR-09 | C | 정상 | 201 "상품이 등록되었습니다." |

## 상품 목록 (GET /api/products) — 공개

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-10 | S | 정상·삭제·숨김 상품 혼재 | **삭제·숨김 제외**, id DESC 정렬 목록 |
| PR-11 | S | 상품 없음 | 빈 리스트 |
| PR-12 | C | 호출 | 200, 비인증 허용 |

## 상품 검색 (GET /api/products/search) — 공개

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-20 | S | 조건 전부 null(빈 검색) | 삭제·숨김 제외 전체 |
| PR-21 | S | keyword가 **title** 부분일치 | 매칭 목록 |
| PR-22 | S | keyword가 **description** 부분일치 | 매칭(제목·설명 OR 검색) |
| PR-23 | S | ⭐ keyword 대소문자 다름("ABC" 등록, "abc" 검색) | 매칭(양쪽 lower) |
| PR-24 | S | categoryId 지정 | 해당 카테고리만 |
| PR-25 | S | minPrice·maxPrice 범위 | 경계 포함(`>=`,`<=`) |
| PR-26 | S | minPrice < 0 또는 maxPrice < 0 | `INVALID_SEARCH_CONDITION` |
| PR-27 | S | minPrice > maxPrice | `INVALID_SEARCH_CONDITION` |
| PR-28 | S | ⭐ minPrice == maxPrice | 허용(해당 가격만) |
| PR-29 | S | tradeStatus="ON_SALE" 등 유효값 | 해당 상태만 |
| PR-30 | S | tradeStatus 잘못된 문자열("FOO") | `INVALID_TRADE_STATUS` |
| PR-31 | S | tradeStatus="  "(공백, 非null) | `INVALID_TRADE_STATUS` |
| PR-32 | S | 검색 결과에도 **삭제·숨김 제외**되는지 | 제외 확인 |
| PR-33 | R | `ProductSpecification.search` + Sort(id desc)로 위 필터/정렬이 실제 쿼리에서 동작 | 기대 목록 |

## 상품 상세 (GET /api/products/{id}) — 공개

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-40 | S | 정상 상품 | ProductResponse, **viewCount 1 증가** |
| PR-41 | S | 없음 | `PRODUCT_NOT_FOUND` (404) |
| PR-42 | S | deletedAt != null | `DELETED_PRODUCT` (404) |
| PR-43 | S | hidden=true | `HIDDEN_PRODUCT` (403) |
| PR-44 | S | ⭐ 삭제 **&** 숨김 동시 | `DELETED_PRODUCT` (deleted 먼저) |
| PR-45 | C | 정상 | 200 |

## 상품 수정 (PATCH /api/products/{id}) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-50 | S | 소유자·ON_SALE·정상 입력 | 필드 변경됨 |
| PR-51 | S | title 공백 / price null·음수 | `INVALID_PRODUCT_TITLE` / `INVALID_PRODUCT_PRICE` |
| PR-52 | S | 없음 | `PRODUCT_NOT_FOUND` |
| PR-53 | S | 삭제됨 | `DELETED_PRODUCT` |
| PR-54 | S | 타인의 상품 | `PRODUCT_OWNER_ONLY` (403) |
| PR-55 | S | 거래완료 상품 | `CANNOT_UPDATE_COMPLETED_PRODUCT` |
| PR-56 | S | 카테고리 없음(소유·미완료 통과 후) | `CATEGORY_NOT_FOUND` |
| PR-57 | S | ⭐ 타인 상품 **&** 거래완료 | `PRODUCT_OWNER_ONLY` (owner가 completed보다 먼저) |

## 상품 삭제 (DELETE /api/products/{id}) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-60 | S | 소유자 | `deletedAt != null`(soft delete) |
| PR-61 | S | 없음 / 이미 삭제 / 타인 | `PRODUCT_NOT_FOUND` / `DELETED_PRODUCT` / `PRODUCT_OWNER_ONLY` |

## 거래 상태 변경 (PATCH /api/products/{id}/status) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-70 | S | ON_SALE → RESERVED (소유자) | 상태 변경 |
| PR-71 | S | request null / tradeStatus 공백 / 잘못된 값 | `INVALID_TRADE_STATUS` |
| PR-72 | S | 없음 / 삭제 / 타인 | `PRODUCT_NOT_FOUND` / `DELETED_PRODUCT` / `PRODUCT_OWNER_ONLY` |
| PR-73 | S | COMPLETED 상품 → ON_SALE 요청 | `CANNOT_CHANGE_COMPLETED_PRODUCT` |
| PR-74 | S | ⭐ COMPLETED 상품 → COMPLETED 요청 | 허용(동일 상태, no-op) |
| PR-75 | S | ⭐ 현재와 동일 상태 요청(ON_SALE→ON_SALE) | 허용, `changeTradeStatus` 미호출(불필요 변경 없음) |

## 내 상품 목록 (GET /api/members/me/products) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| PR-80 | S | 내 상품들(삭제·숨김 혼재) | **삭제만 제외, 숨김은 포함**, id DESC |
| PR-81 | C | 비인증 | 401 |

## 리포지토리 (ProductRepository, @DataJpaTest)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| PR-90 | R | `findAllByDeletedAtIsNullAndHiddenFalseOrderByIdDesc` | 삭제·숨김 제외, id 내림차순 |
| PR-91 | R | `findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc` | 카테고리+필터 |
| PR-92 | R | `findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc` | 회원+삭제제외(숨김 포함) |
| PR-93 | R | `existsByIdAndDeletedAtIsNullAndHiddenFalse` | 정상/숨김/삭제 각각 true/false |

## 인가

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| PR-95 | C | GET 목록·검색·상세·카테고리별 | 비인증 허용 |
| PR-96 | C | POST·PATCH·DELETE 비인증 | 401 |

## 비고 / 주의

- PR-08, PR-44, PR-57은 **검증 순서 회귀 방지**용. 순서가 바뀌면 다른 ErrorCode가 나옴.
- ⚠️ **삭제·숨김 상품에 신고/관심/댓글이 가능**한지는 각 도메인(05·06·07) 갭으로 다룸. 상품 도메인은 자기 엔드포인트만.
- `validateAccessibleProduct`(존재·미삭제·미숨김 아니면 `PRODUCT_NOT_FOUND`)가 현재 외부에서 호출되는지 확인 후, 호출처가 없으면 S 단위로 동작만 검증하고 "미사용" 메모.
