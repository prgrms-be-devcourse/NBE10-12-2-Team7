# 06. Favorite (관심상품) — 담당: 권건우

대상: `FavoriteService`, `FavoriteController`, `FavoriteRepository`, `Favorite`(UNIQUE 제약)
관련 ErrorCode: `FAVORITE_ALREADY_EXISTS`, `FAVORITE_NOT_FOUND`, `PRODUCT_NOT_FOUND`

## 핵심 로직 요약

- 등록: `validateFavoriteCreatable`(상품 `existsById` 없으면 `PRODUCT_NOT_FOUND`, 이미 등록 `existsByMemberIdAndProductId`면 `FAVORITE_ALREADY_EXISTS`) → save. save 시 UNIQUE 위반(`DataIntegrityViolationException`)도 `FAVORITE_ALREADY_EXISTS`로 변환(race condition).
- 목록: `findAllByMemberIdOrderByCreatedAtDescIdDesc`(최근 등록순, id tiebreaker).
- 취소: `findByMemberIdAndProductId`(없으면 `FAVORITE_NOT_FOUND`) → delete(**하드 삭제**).
- 테이블 UNIQUE: `uk_favorites_member_product(member_id, product_id)`.
- ⚠️ 숨김·삭제 상품도 `existsById`로 통과 → 관심 등록 가능(전략 §8 갭).

## 관심 등록 (POST /api/products/{productId}/favorites) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| FV-01 | S | 존재 상품 + 미등록 | 저장, FavoriteResponse |
| FV-02 | S | 상품 없음 | `PRODUCT_NOT_FOUND` (404) |
| FV-03 | S | 이미 등록(`existsByMemberIdAndProductId=true`) | `FAVORITE_ALREADY_EXISTS` (409) |
| FV-04 | S | ⭐ 사전체크 통과 후 save에서 `DataIntegrityViolationException`(UNIQUE 위반) | `FAVORITE_ALREADY_EXISTS` (race condition 변환) |
| FV-05 | C | 정상 | 201/200(규약 확인) |
| FV-06 | S | ⚠️ 숨김/삭제 상품 등록 시도 | **현재: 허용** — `// KNOWN GAP` |

## 내 관심 목록 (GET /api/members/me/favorites) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| FV-10 | S | 여러 관심상품 | **최근 등록순(createdAt DESC, id DESC)** |
| FV-11 | S | 없음 | 빈 리스트 |
| FV-12 | S | ⚠️ 관심 등록 후 상품이 삭제됨 | **현재: 목록에 그대로 노출**(조인 필터 없음) — `// KNOWN GAP` |

## 관심 취소 (DELETE /api/products/{productId}/favorites) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| FV-20 | S | 등록되어 있던 관심상품 | 삭제됨(repository.delete 호출) |
| FV-21 | S | 등록 안 된 상품 취소 | `FAVORITE_NOT_FOUND` (404) |

## 리포지토리 (FavoriteRepository, @DataJpaTest)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| FV-30 | R | `existsByMemberIdAndProductId` | true/false |
| FV-31 | R | `findByMemberIdAndProductId` | 채워짐/빈값 |
| FV-32 | R | `findAllByMemberIdOrderByCreatedAtDescIdDesc` | 최근순 정렬 |
| FV-33 | R | ⭐ 동일 (member_id, product_id) 중복 insert | UNIQUE 제약 위반(`DataIntegrityViolationException`) |

## 인가

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| FV-40 | C | 등록·목록·취소 비인증 | 401 |

## 비고

- FV-04와 FV-33은 짝: FV-33이 DB UNIQUE 제약 존재를 보장, FV-04는 그 예외를 서비스가 올바른 ErrorCode로 변환하는지.
- 취소는 soft delete가 아니라 **하드 delete**임에 유의(다른 도메인과 다름).
