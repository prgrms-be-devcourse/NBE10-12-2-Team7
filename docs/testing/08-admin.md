# 08. Admin (관리자) — 담당: 팀장

대상: `AdminMemberService`, `AdminProductService`, `AdminCommentService`, `AdminReportService`, `AdminDashboardService` + 각 컨트롤러/리포지토리
관련 ErrorCode: `MEMBER_NOT_FOUND`, `PRODUCT_NOT_FOUND`, `COMMENT_NOT_FOUND`, `REPORT_NOT_FOUND`, `INVALID_MEMBER_STATUS`, `INVALID_REPORT_STATUS`

> 모든 `/api/admin/**`는 `hasRole("ADMIN")`. 인가는 각 섹션 공통: **USER→403, 비인증→401**.
> 상태 변경 요청 DTO는 `@Valid` 없음 → 파싱은 서비스에서(`parseStatus`, **`trim()` 후 valueOf**).
> ⚠️ 알려진 갭 N1·N3·집계 포함 — 전략 §8.

## 8.1 회원 관리 (AdminMemberService)

조회는 상태 무관 전체. 상태 변경은 `changeStatus`(DELETED면 `deletedAt=now()`, 그 외엔 `deletedAt=null`로 **클리어**).

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| AD-01 | S | getMembers (ACTIVE/SUSPENDED/DELETED 혼재) | **전체** 반환(상태 무관) |
| AD-02 | S | getMember 존재 | AdminMemberResponse |
| AD-03 | S | getMember 없음 | `MEMBER_NOT_FOUND` |
| AD-04 | S | status="SUSPENDED"로 변경 | status=SUSPENDED |
| AD-05 | S | status="DELETED"로 변경 | status=DELETED, **deletedAt != null** |
| AD-06 | S | ⭐ DELETED 회원을 "ACTIVE"로 변경 | status=ACTIVE, **deletedAt = null**(클리어) |
| AD-07 | S | 대상 회원 없음 | `MEMBER_NOT_FOUND` |
| AD-08 | S | status=null / "" / 공백 | `INVALID_MEMBER_STATUS` (400) |
| AD-09 | S | status="FOO"(잘못된 값) | `INVALID_MEMBER_STATUS` |
| AD-10 | S | ⭐ status=" SUSPENDED "(앞뒤 공백) | 통과(`trim()` 후 파싱) |
| AD-11 | S | ⚠️ **N1**: 관리자가 다른 ROLE_ADMIN/시드관리자/본인을 SUSPENDED·DELETED | **현재: 허용**(가드 없음) — `// KNOWN GAP N1` |
| AD-12 | C | USER 토큰으로 호출 | 403 |
| AD-13 | C | 비인증 | 401 |

## 8.2 상품 관리 (AdminProductService)

조회는 숨김·삭제 무관 전체. 숨김은 한방향(`hide()`), 삭제는 soft(`softDelete()`). 작성자 아니어도 가능.

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| AD-20 | S | getProducts (숨김·삭제 혼재) | 전체 반환 |
| AD-21 | S | getProduct 없음 | `PRODUCT_NOT_FOUND` |
| AD-22 | S | hideProduct 정상 | `hidden=true` |
| AD-23 | S | hideProduct 없음 | `PRODUCT_NOT_FOUND` |
| AD-24 | S | ⭐ 이미 hidden인 상품 숨김 | 그대로 `hidden=true`(멱등) |
| AD-25 | S | deleteProduct 정상 | `deletedAt != null` |
| AD-26 | S | deleteProduct 없음 | `PRODUCT_NOT_FOUND` |
| AD-27 | S | ⚠️ 삭제(soft)된 상품을 다시 hide/delete | **현재: 성공**(findById가 soft-delete 반환) — `// KNOWN GAP` |
| AD-28 | C | USER/비인증 | 403 / 401 |

## 8.3 댓글 관리 (AdminCommentService)

목록은 삭제 포함 전체. 삭제는 `findByIdAndDeletedAtIsNull` 사용 → 이미 삭제된 댓글은 not found.

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| AD-30 | S | getComments (삭제 포함) | 전체 반환 |
| AD-31 | S | deleteComment 정상(작성자 아님) | soft delete 성공(관리자는 소유자 불문) |
| AD-32 | S | deleteComment 없음 | `COMMENT_NOT_FOUND` |
| AD-33 | S | ⭐ 이미 삭제된 댓글 삭제 | `COMMENT_NOT_FOUND`(findByIdAndDeletedAtIsNull) |
| AD-34 | C | USER/비인증 | 403 / 401 |

## 8.4 신고 관리 (AdminReportService)

상태 전이 규칙 없음(어떤 상태→어떤 상태든 허용).

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| AD-40 | S | getReports | 전체 |
| AD-41 | S | getReport 없음 | `REPORT_NOT_FOUND` |
| AD-42 | S | status="REVIEWING"/"COMPLETED"/"REJECTED" | 해당 상태로 변경 |
| AD-43 | S | 신고 없음 | `REPORT_NOT_FOUND` |
| AD-44 | S | status=null/""/공백 | `INVALID_REPORT_STATUS` |
| AD-45 | S | status="FOO" | `INVALID_REPORT_STATUS` |
| AD-46 | S | ⭐ status=" completed " 대문자/공백 → trim. (※ valueOf는 대소문자 구분 — "completed"는 실패) | `trim()`만 적용. 소문자 값은 `INVALID_REPORT_STATUS` |
| AD-47 | S | ⚠️ COMPLETED→RECEIVED 역행 | **현재: 허용**(전이 규칙 없음) — `// KNOWN GAP` 여부 팀 결정 |
| AD-48 | C | USER/비인증 | 403 / 401 |

## 8.5 대시보드 (AdminDashboardService)

`count()` 전체 + `countByStatus(RECEIVED)`.

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| AD-50 | S | 회원·상품·신고·댓글·RECEIVED신고 수 집계 | 각 count 값 정확 |
| AD-51 | S | ⚠️ **N3**: pendingReports는 **RECEIVED만**(REVIEWING 제외) | 현재 동작 고정 — `// KNOWN GAP N3` |
| AD-52 | S | ⚠️ totalMembers/totalProducts에 **soft-deleted·hidden 포함** | 현재 동작 고정 — `// KNOWN GAP` |
| AD-53 | C | USER/비인증 | 403 / 401 |

## 리포지토리 (Admin*Repository, @DataJpaTest)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| AD-60 | R | Admin*Repository `findAll`/`count` | 상태 무관 전체/카운트 |
| AD-61 | R | AdminReportRepository `countByStatus(RECEIVED)` | RECEIVED 수만 |
| AD-62 | R | AdminCommentRepository `findByIdAndDeletedAtIsNull` | 삭제 제외 |

## 비고 / 기존 자산

- 메모리 기록상 어드민 테스트는 이미 green(2026-06-29 재검증). 본 문서는 **기존 테스트와의 갭(N1·N2·N3)을 명시적으로 케이스화**하는 데 목적.
- **N1·N3 처리 방침 결정 필요**(팀장): ⓐ 현재 동작 고정 + `// KNOWN GAP` (권장, 빠른 그린) / ⓑ 기대 동작으로 `@Disabled` 작성 후 추후 구현 / ⓒ 지금 수정.
- AD-46 주의: `MemberStatus`/`ReportStatus`의 `valueOf`는 **대소문자 구분**. 서비스는 `trim()`만 하고 `toUpperCase()`는 하지 않음 → 소문자 입력은 실패. 이 동작을 테스트로 고정.
