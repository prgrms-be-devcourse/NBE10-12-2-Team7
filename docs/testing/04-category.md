# 04. Category (카테고리·카테고리별 상품) — 담당: 한상민

대상: `CategoryService`, `CategoryController`, `CategoryRepository`, `CategoryInitializer`
관련 ErrorCode: `CATEGORY_NOT_FOUND`

## 핵심 로직 요약

- `getCategories`: `findAllByOrderByIdAsc` → CategoryResponse 매핑. **공개**.
- 카테고리별 상품(GET /api/categories/{id}/products)은 `ProductService.getProductsByCategory` 위임:
  - 카테고리 미존재 → `CATEGORY_NOT_FOUND`
  - 존재 → `deletedAt IS NULL AND hidden=false` 필터, id DESC.
- `CategoryInitializer`가 시작 시 기본 카테고리 시드(기존 테스트 `CategoryInitializerTest` 존재).

## 카테고리 목록 (GET /api/categories) — 공개

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| CA-01 | S | 여러 카테고리 존재 | **id 오름차순** 목록 |
| CA-02 | S | 비어있음 | 빈 리스트 |
| CA-03 | C | 호출 | 200, 비인증 허용 |

## 카테고리별 상품 (GET /api/categories/{id}/products) — 공개

> 서비스 로직은 `ProductService.getProductsByCategory` (03-product PR 표와 중복되지 않게 여기서는 카테고리 관점만)

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| CA-10 | S | 존재하는 카테고리 | 해당 카테고리의 **삭제·숨김 제외** 상품, id DESC |
| CA-11 | S | 존재하지 않는 카테고리 | `CATEGORY_NOT_FOUND` (404) |
| CA-12 | S | 상품 없는 카테고리 | 빈 리스트 |
| CA-13 | C | 정상/없는 카테고리 | 200 / 404 |

## 리포지토리 (CategoryRepository, @DataJpaTest)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| CA-20 | R | `findAllByOrderByIdAsc` | id 오름차순 |
| CA-21 | R | `existsById` | true/false |

## Initializer (CategoryInitializer)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| CA-30 | I | 앱 시작 시 기본 카테고리 시드 | 정의된 개수/이름 생성 |
| CA-31 | I | 이미 시드된 상태로 재실행 | 중복 생성 안 함(멱등) — *현재 구현의 멱등성 확인 필요* |

## 비고

- CA-31의 멱등성은 `CategoryInitializer` 구현을 확인해 실제 보장 여부를 테스트로 고정(중복 INSERT 여부).
- 카테고리별 상품 필터 로직은 product와 공유되므로, **필터/정렬 정밀 검증은 03-product(PR-33, PR-91)에서**, 여기서는 카테고리 존재/위임만 가볍게.
