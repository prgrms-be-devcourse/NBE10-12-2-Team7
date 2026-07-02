# 05. Comment (댓글 CRUD) — 담당: 권건우

대상: `CommentService`, `CommentController`, `CommentRepository`
관련 ErrorCode: `COMMENT_NOT_FOUND`, `COMMENT_OWNER_ONLY`, `PRODUCT_NOT_FOUND`

## 핵심 로직 요약

- 작성: `productRepository.existsById`(없으면 `PRODUCT_NOT_FOUND`) → `Comment.of(memberId, productId, content)` 저장.
- 목록: 상품 존재 검사 → `findAllByProductIdAndDeletedAtIsNullOrderByCreatedAtAsc`(**삭제 제외, 작성순**). **공개**.
- 수정/삭제: `findByIdAndDeletedAtIsNull`(없거나 삭제됨 → `COMMENT_NOT_FOUND`) → **작성자 일치 검사**(불일치 → `COMMENT_OWNER_ONLY`) → 수정/`softDelete`.
- ⚠️ 작성 시 `existsById`만 검사 → **숨김·삭제(soft) 상품에도 댓글 가능**(전략 §8 갭).
- ⚠️ 회원 상태(정지/탈퇴)는 검사하지 않음(갭 N2).

## 댓글 작성 (POST /api/products/{productId}/comments) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| CM-01 | S | 존재하는 상품 + 정상 content | 저장(memberId·productId·content), CommentResponse |
| CM-02 | S | 상품 없음(`existsById=false`) | `PRODUCT_NOT_FOUND` (404) |
| CM-03 | C | content 공백(`@NotBlank`) | 400 / `COMMON_002` |
| CM-04 | C | content 501자(`@Size max 500`) | 400 |
| CM-05 | C | ⭐ content 정확히 500자 | 통과 |
| CM-06 | C | 정상 | 201/200(컨트롤러 규약 확인) |
| CM-07 | S | ⚠️ 숨김/삭제(soft) 상품에 작성 | **현재: 허용**(`existsById`는 soft-delete 무시) — `// KNOWN GAP` |

## 댓글 목록 (GET /api/products/{productId}/comments) — 공개

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| CM-10 | S | 댓글 다수(삭제 일부 포함) | **삭제 제외**, **작성순(createdAt ASC)** |
| CM-11 | S | 상품 없음 | `PRODUCT_NOT_FOUND` |
| CM-12 | S | 댓글 없음 | 빈 리스트 |
| CM-13 | C | 비인증 호출 | 허용(공개) |

## 댓글 수정 (PATCH /api/comments/{commentId}) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| CM-20 | S | 작성자 본인 | content 변경됨 |
| CM-21 | S | 댓글 없음 | `COMMENT_NOT_FOUND` |
| CM-22 | S | ⭐ 이미 삭제된 댓글(`findByIdAndDeletedAtIsNull` 빈값) | `COMMENT_NOT_FOUND` |
| CM-23 | S | 타인의 댓글 | `COMMENT_OWNER_ONLY` (403) |
| CM-24 | C | content 공백 / 501자 | 400 |

## 댓글 삭제 (DELETE /api/comments/{commentId}) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| CM-30 | S | 작성자 본인 | `deletedAt != null`(soft delete) |
| CM-31 | S | 없음 / 이미 삭제 | `COMMENT_NOT_FOUND` |
| CM-32 | S | 타인의 댓글 | `COMMENT_OWNER_ONLY` |

## 리포지토리 (CommentRepository, @DataJpaTest)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| CM-40 | R | `findAllByProductIdAndDeletedAtIsNullOrderByCreatedAtAsc` | 삭제 제외 + 작성순 |
| CM-41 | R | `findByIdAndDeletedAtIsNull`: 정상/삭제됨 | 채워짐/빈값 |

## 인가

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| CM-50 | C | 목록 비인증 | 허용 |
| CM-51 | C | 작성·수정·삭제 비인증 | 401 |

## 비고

- CM-22가 함정: 수정/삭제는 `findByIdAndDeletedAtIsNull`이라 **이미 삭제된 댓글은 "없음"으로 취급**됨. ID로는 행이 있지만 테스트에서는 `COMMENT_NOT_FOUND`를 기대.
- CM-40의 정렬은 작성순(ASC). createdAt 동일 시각 충돌 가능성은 전략 §3 참고(가능하면 명확히 시차를 두고 저장).
