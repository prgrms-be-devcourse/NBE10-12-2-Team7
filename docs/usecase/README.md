# 유즈케이스 문서

동네마켓 백엔드의 유저스토리·유즈케이스 정의서입니다. 각 유즈케이스는 **기본 흐름 / 예외 흐름 / 비즈니스 규칙 / 데이터 / API / 테스트 관점**으로 구성됩니다.
"테스트 관점" 행은 [docs/testing](../testing/README.md)의 테스트 케이스 ID(예: `AU-01`)와 연결됩니다.

## 1. Actor 정의

| Actor | 설명 |
| --- | --- |
| 비회원 (Guest) | 로그인하지 않은 사용자. 회원가입·로그인과 공개 조회(상품 목록·검색·상세, 카테고리, 댓글 목록)만 가능 |
| 일반회원 (USER) | `ROLE_USER`·`ACTIVE` 회원. 상품/댓글/관심/신고 등록 및 본인 자원 관리. JWT 토큰으로 인증 |
| 작성자 (Owner) | USER 중 해당 자원(상품·댓글)을 등록한 본인. 수정·삭제 권한의 기준 |
| 관리자 (ADMIN) | `ROLE_ADMIN` 회원. `/api/admin/**` 접근. 회원·상품·댓글·신고 관리 및 대시보드 |
| 시스템 (System) | 앱 기동 시 카테고리·관리자 계정을 시드하는 Initializer |

> 상태 제약: `SUSPENDED`/`DELETED` 회원은 **로그인 불가**. 단, 이미 발급된 토큰의 무효화는 미구현(테스트 전략 §8 갭 N2).

---

## 2. 유즈케이스 목록

| ID | Actor | Use Case | 설명 | 우선순위 | 담당자 |
| --- | --- | --- | --- | --- | --- |
| UC-001 | Guest | 회원가입 | 이메일·비번·닉네임으로 일반회원 생성 | High | 김대연 |
| UC-002 | Guest | 로그인 | 자격 검증 후 JWT 발급 | High | 김대연 |
| UC-003 | USER | 내 정보 조회 | 본인 프로필 조회 | Medium | 김대연 |
| UC-004 | USER | 내 정보 수정 | 닉네임 변경 | Medium | 김대연 |
| UC-005 | USER | 회원 탈퇴 | 본인 계정 soft delete | Medium | 김대연 |
| UC-006 | USER | 상품 등록 | 판매 상품 등록 | High | 한상민 |
| UC-007 | Guest | 상품 목록 조회 | 판매중(미삭제·미숨김) 목록 | High | 한상민 |
| UC-008 | Guest | 상품 검색 | 키워드·카테고리·가격·상태 검색 | High | 한상민 |
| UC-009 | Guest | 상품 상세 조회 | 상세 + 조회수 증가 | High | 한상민 |
| UC-010 | Owner | 상품 수정 | 작성자가 상품 정보 수정 | High | 한상민 |
| UC-011 | Owner | 상품 삭제 | 작성자가 상품 soft delete | Medium | 한상민 |
| UC-012 | Owner | 거래 상태 변경 | ON_SALE/RESERVED/COMPLETED 전환 | High | 한상민 |
| UC-013 | USER | 내 상품 목록 조회 | 본인 등록 상품(숨김 포함) | Medium | 한상민 |
| UC-014 | Guest | 카테고리 목록 조회 | 전체 카테고리 | Low | 한상민 |
| UC-015 | Guest | 카테고리별 상품 조회 | 특정 카테고리 상품 | Medium | 한상민 |
| UC-016 | USER | 댓글 작성 | 상품에 댓글 등록 | Medium | 권건우 |
| UC-017 | Guest | 댓글 목록 조회 | 상품의 댓글(미삭제) 조회 | Medium | 권건우 |
| UC-018 | Owner | 댓글 수정 | 작성자가 댓글 수정 | Low | 권건우 |
| UC-019 | Owner | 댓글 삭제 | 작성자가 댓글 soft delete | Low | 권건우 |
| UC-020 | USER | 관심상품 등록 | 상품을 관심목록에 추가 | Medium | 권건우 |
| UC-021 | USER | 내 관심목록 조회 | 본인 관심상품 목록 | Medium | 권건우 |
| UC-022 | USER | 관심상품 취소 | 관심목록에서 제거 | Medium | 권건우 |
| UC-023 | USER | 상품 신고 | 부적절 상품 신고 | Medium | 서유진 |
| UC-024 | USER | 회원 신고 | 부적절 회원 신고 | Medium | 서유진 |
| UC-025 | USER | 내 신고내역 조회 | 본인 신고 목록 | Low | 서유진 |
| UC-026 | ADMIN | 회원 목록 조회 | 전체 회원(상태 무관) | Medium | 팀장 |
| UC-027 | ADMIN | 회원 상세 조회 | 회원 단건 | Low | 팀장 |
| UC-028 | ADMIN | 회원 상태 변경 | ACTIVE/SUSPENDED/DELETED | High | 팀장 |
| UC-029 | ADMIN | 상품 목록 조회 | 전체 상품(숨김·삭제 포함) | Medium | 팀장 |
| UC-030 | ADMIN | 상품 상세 조회 | 상품 단건 | Low | 팀장 |
| UC-031 | ADMIN | 상품 숨김 | 상품 노출 차단(한방향) | High | 팀장 |
| UC-032 | ADMIN | 상품 삭제 | 관리자 soft delete | Medium | 팀장 |
| UC-033 | ADMIN | 댓글 목록 조회 | 전체 댓글(삭제 포함) | Low | 팀장 |
| UC-034 | ADMIN | 댓글 삭제 | 관리자 soft delete | Medium | 팀장 |
| UC-035 | ADMIN | 신고 목록 조회 | 전체 신고 | Medium | 팀장 |
| UC-036 | ADMIN | 신고 상세 조회 | 신고 단건 | Low | 팀장 |
| UC-037 | ADMIN | 신고 상태 변경 | RECEIVED/REVIEWING/COMPLETED/REJECTED | High | 팀장 |
| UC-038 | ADMIN | 대시보드 조회 | 회원·상품·신고·댓글 집계 | Medium | 팀장 |

---

## 3. 도메인별 상세 문서

| 문서 | UC 범위 | 담당 |
| --- | --- | --- |
| [01-auth.md](01-auth.md) | UC-001 ~ UC-002 | 김대연 |
| [02-member.md](02-member.md) | UC-003 ~ UC-005 | 김대연 |
| [03-product.md](03-product.md) | UC-006 ~ UC-013 | 한상민 |
| [04-category.md](04-category.md) | UC-014 ~ UC-015 | 한상민 |
| [05-comment.md](05-comment.md) | UC-016 ~ UC-019 | 권건우 |
| [06-favorite.md](06-favorite.md) | UC-020 ~ UC-022 | 권건우 |
| [07-report.md](07-report.md) | UC-023 ~ UC-025 | 서유진 |
| [08-admin.md](08-admin.md) | UC-026 ~ UC-038 | 팀장 |

---

## 4. 공통 테스트 관점 (모든 UC 적용)

문서 하단 요구사항 반영:

- **유저 플로우 전체 테스트**: 회원가입 → 로그인 → 상품등록 → 조회 → 거래완료 같은 end-to-end 흐름(통합 테스트 `I`).
- **엣지 케이스(입력 방어)**: 경계값(범위 끝 값), null/공백, 잘못된 enum, 음수, 길이 초과 — 입력되면 안 되는 값에 대한 방어.
- **도메인별 테스트**: 각 도메인 비즈니스 규칙 단위 검증(서비스 `S`).
- 상세 케이스·계층(S/C/R/I)은 [docs/testing](../testing/README.md) 참조.
