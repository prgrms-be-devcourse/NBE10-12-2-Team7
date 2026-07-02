# 07. Report (상품/회원 신고) — 담당: 서유진

대상: `ReportService`, `ReportController`, `ReportRepository`, `Report`
관련 ErrorCode: `REPORT_NOT_FOUND`, `CANNOT_REPORT_OWN_PRODUCT`, `CANNOT_REPORT_SELF`, `DUPLICATE_REPORT`, `PRODUCT_NOT_FOUND`, `MEMBER_NOT_FOUND`

## 핵심 로직 요약

- **상품 신고 순서**: 신고자 존재(`existsById`, 없으면 `MEMBER_NOT_FOUND`) → 상품 조회(없으면 `PRODUCT_NOT_FOUND`) → **본인 상품 금지**(`CANNOT_REPORT_OWN_PRODUCT`) → **중복 금지**(`existsByReporterIdAndTargetProductId` → `DUPLICATE_REPORT`) → `Report.ofProduct`(type=PRODUCT, status=RECEIVED).
- **회원 신고 순서**: 신고자 존재 → **자기 신고 금지**(`reporterId.equals(targetMemberId)` → `CANNOT_REPORT_SELF`) → 대상 회원 존재(`findById`, 없으면 `MEMBER_NOT_FOUND`) → 중복 금지 → `Report.ofMember`(type=MEMBER).
- 내 신고 조회: 신고자 존재 → `findAllByReporterId`(상태·타입 무관 전체).
- 신규 신고는 항상 `status=RECEIVED`.
- ⚠️ 상품 신고 시 `findById`는 **숨김·삭제 상품도 반환** → 삭제(soft)된 상품도 신고 가능(전략 §8 갭).
- ⚠️ 회원 신고 시 대상 상태(정지/탈퇴) 확인 안 함 → 탈퇴 회원도 신고 가능(갭).

## 상품 신고 (POST /api/products/{productId}/reports) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| RP-01 | S | 타인 상품, 첫 신고 | 저장, `reportType=PRODUCT`, `status=RECEIVED` |
| RP-02 | S | 신고자 회원 없음 | `MEMBER_NOT_FOUND` |
| RP-03 | S | 대상 상품 없음 | `PRODUCT_NOT_FOUND` |
| RP-04 | S | 본인이 등록한 상품 | `CANNOT_REPORT_OWN_PRODUCT` (400) |
| RP-05 | S | 이미 신고한 상품(중복) | `DUPLICATE_REPORT` (409) |
| RP-06 | S | ⭐ 본인 상품 **&** 중복 신고 이력 | `CANNOT_REPORT_OWN_PRODUCT`(본인검사가 중복보다 먼저) |
| RP-07 | C | reason null(`@NotNull`) | 400 / `COMMON_002` |
| RP-08 | C | content 501자(`@Size 500`) | 400 |
| RP-09 | C | ⭐ content null(@Size만, 필수 아님) | 통과 |
| RP-10 | C | 정상 | 201 "신고가 접수되었습니다." |
| RP-11 | S | ⚠️ 삭제(soft)된 상품 신고 | **현재: 허용** — `// KNOWN GAP` |

## 회원 신고 (POST /api/members/{memberId}/reports) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| RP-20 | S | 타인 회원, 첫 신고 | 저장, `reportType=MEMBER`, `status=RECEIVED` |
| RP-21 | S | 신고자 회원 없음 | `MEMBER_NOT_FOUND` |
| RP-22 | S | 자기 자신 신고(reporterId==targetMemberId) | `CANNOT_REPORT_SELF` (400) |
| RP-23 | S | 대상 회원 없음 | `MEMBER_NOT_FOUND` |
| RP-24 | S | 이미 신고한 회원(중복) | `DUPLICATE_REPORT` |
| RP-25 | S | ⭐ 자기 신고: 자기검사가 대상존재검사보다 먼저 | `CANNOT_REPORT_SELF` |
| RP-26 | C | reason null | 400 |
| RP-27 | C | 정상 | 201 |
| RP-28 | S | ⚠️ 탈퇴/정지 회원 신고 | **현재: 허용** — `// KNOWN GAP` |

## 내 신고 내역 (GET /api/members/me/reports) — 인증

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| RP-30 | S | 내가 한 신고들(상품·회원·상태 혼재) | 전체 목록(MyReportResponse) |
| RP-31 | S | 신고자 회원 없음 | `MEMBER_NOT_FOUND` |
| RP-32 | S | 신고 이력 없음 | 빈 리스트 |

## 리포지토리 (ReportRepository, @DataJpaTest)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| RP-40 | R | `existsByReporterIdAndTargetProductId` | true/false |
| RP-41 | R | `existsByReporterIdAndTargetMemberId` | true/false |
| RP-42 | R | `findAllByReporterId` | 해당 신고자 신고만 |
| RP-43 | R | `countByStatus(RECEIVED)` | 상태별 카운트(대시보드에서 사용) |

## 인가

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| RP-50 | C | 신고·내역조회 비인증 | 401 |

## 비고

- RP-06, RP-25는 **검증 순서** 회귀 방지.
- 신고 사유(`ReportReason`)는 enum: FAKE_ITEM/FRAUD_SUSPECTED/PROHIBITED_ITEM/INAPPROPRIATE_CONTENT/ETC. `@RequestBody`로 잘못된 enum 문자열이 오면 Jackson 역직렬화 단계에서 실패(→ 500/Exception 핸들러) — 이 동작을 C 테스트로 확인할지 팀 결정(현재 별도 처리 없음).
