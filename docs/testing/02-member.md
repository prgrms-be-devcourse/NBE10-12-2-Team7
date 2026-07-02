# 02. Member (내 정보 조회·수정·탈퇴) — 담당: 김대연

대상: `MemberService`, `MemberController`, `MemberRepository`
관련 ErrorCode: `MEMBER_NOT_FOUND`, `DELETED_MEMBER`, `SUSPENDED_MEMBER`, `DUPLICATE_NICKNAME`

## 핵심 로직 요약

- 세 엔드포인트 모두 `findById` → **`validateActiveMember`**(DELETED→`DELETED_MEMBER`, SUSPENDED→`SUSPENDED_MEMBER`) 공통 선검사.
- 수정: 활성검사 후 **`existsByNicknameAndIdNot`** (본인 제외 중복) → `member.update(nickname)`.
- 탈퇴: `softDelete()` → status=DELETED, `deletedAt=now()`.
- 전부 `@AuthenticationPrincipal Long memberId` 기반(본인만).

## 내 정보 조회 (GET /api/members/me)

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| ME-01 | S | ACTIVE 회원 | MemberResponse 반환(이메일·닉네임·role 등) |
| ME-02 | S | id로 회원 없음 | `MEMBER_NOT_FOUND` (404) |
| ME-03 | S | status=DELETED | `DELETED_MEMBER` (400) |
| ME-04 | S | status=SUSPENDED | `SUSPENDED_MEMBER` (403) |

## 내 정보 수정 (PATCH /api/members/me)

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| ME-10 | S | ACTIVE + 새 닉네임 중복 아님 | 닉네임 변경됨, MemberResponse |
| ME-11 | S | 회원 없음 | `MEMBER_NOT_FOUND` |
| ME-12 | S | DELETED / SUSPENDED | `DELETED_MEMBER` / `SUSPENDED_MEMBER` |
| ME-13 | S | `existsByNicknameAndIdNot=true`(타인이 사용 중) | `DUPLICATE_NICKNAME` (409) |
| ME-14 | S | ⭐ **자기 현재 닉네임 그대로** 제출 (IdNot이 본인 제외) | 허용 — 변경 성공/무변화 |
| ME-15 | C | nickname 공백 / 1자 / 21자 (`@Size 2~20`) | 400 / `COMMON_002` |
| ME-16 | C | 정상 요청 | 200, 서비스 호출 검증 |

## 내 정보 탈퇴 (DELETE /api/members/me)

| ID | 계층 | 시나리오 | 기대 결과 |
|---|---|---|---|
| ME-20 | S | ACTIVE 회원 | `status=DELETED`, `deletedAt != null` |
| ME-21 | S | 회원 없음 | `MEMBER_NOT_FOUND` |
| ME-22 | S | 이미 DELETED | `DELETED_MEMBER` (재탈퇴 방지) |
| ME-23 | S | SUSPENDED | `SUSPENDED_MEMBER` |
| ME-24 | C | 정상 | 200/204(컨트롤러 응답 규약 확인), 서비스 호출 검증 |

## 인가

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| ME-30 | C | 비인증으로 `/api/members/me` 접근 | 401 |

## 리포지토리 (MemberRepository, @DataJpaTest)

| ID | 계층 | 시나리오 | 기대 |
|---|---|---|---|
| ME-40 | R | `existsByEmail` 존재/미존재 | true/false |
| ME-41 | R | `existsByNickname` 존재/미존재 | true/false |
| ME-42 | R | `existsByNicknameAndIdNot`: 같은 닉네임을 **본인이** 가진 경우 | **false**(본인 제외) |
| ME-43 | R | `existsByNicknameAndIdNot`: 같은 닉네임을 **타인이** 가진 경우 | true |
| ME-44 | R | `findByEmail` 존재/미존재 | Optional 채워짐/비어있음 |

## 비고

- ME-14, ME-42가 본 도메인의 함정 포인트: "닉네임 중복" 로직이 본인을 중복으로 오판하지 않는지.
- 정지/탈퇴 회원이 **member 외 다른 도메인**(상품 등록 등)에 접근하는 시나리오는 본 도메인 범위 밖이며, 전략 문서 §8 갭 N2로 별도 관리.
