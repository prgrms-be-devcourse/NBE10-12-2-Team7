# 테스트 명세 (Test Spec)

백엔드 구현이 "제대로 작동하는가"를 검증하기 위한 **테스트 설계 문서**입니다.
테스트 코드를 짜기 전에 이 문서로 **무엇을(유즈케이스·엣지케이스) / 어느 계층에서 / 어떤 결과로** 검증할지 합의합니다.
각 표의 **한 행(row) = 최소 하나의 테스트 메서드**가 되도록 작성했습니다.

## 읽는 순서

1. **[00-test-strategy.md](00-test-strategy.md)** — 먼저 읽으세요. 테스트 계층 분담, 픽스처(객체 생성) 전략, H2/MySQL 차이, 인가 매트릭스, 컨벤션, DoD. **모든 도메인 공통 규칙.**
2. 자기 담당 도메인 문서 → 케이스 표를 보고 테스트 작성.

## 도메인별 문서 & 담당

| 문서 | 도메인 | 담당 | 핵심 검증 포인트 |
|---|---|---|---|
| [01-auth.md](01-auth.md) | 회원가입·로그인 | 김대연 | 중복/상태/비번 검증 순서, 토큰 발급, race condition |
| [02-member.md](02-member.md) | 내 정보 조회·수정·탈퇴 | 김대연 | 활성회원 검증, 닉네임 중복(본인 제외), soft delete |
| [03-product.md](03-product.md) | 상품 CRUD·검색·상태 | 한상민 | 소유자 검증, 거래완료 제약, 검색 Specification, 조회수 |
| [04-category.md](04-category.md) | 카테고리·카테고리별 상품 | 한상민 | 정렬, 카테고리별 상품(숨김/삭제 제외) |
| [05-comment.md](05-comment.md) | 댓글 CRUD | 권건우 | 작성자 검증, soft delete 필터, 정렬 |
| [06-favorite.md](06-favorite.md) | 관심상품 | 권건우 | 중복(UNIQUE 제약), race condition, 정렬 |
| [07-report.md](07-report.md) | 상품/회원 신고 | 서유진 | 자기 신고 금지, 중복 신고, 검증 순서 |
| [08-admin.md](08-admin.md) | 관리자(회원·상품·댓글·신고·대시보드) | 팀장 | 권한(ADMIN), 상태 전이, 집계, 알려진 갭 N1~N3 |

### 부속 문서

- **[09-base-init-data.md](09-base-init-data.md)** — 개발/검증용 초기 시드 데이터 **제안서(미도입)**. 관리자 콘솔 전 화면 상태를 부팅 한 번으로 검증하기 위한 회원·상품·댓글·신고 시드 설계. 승인 후 도입.

## 표기 규칙

- **계층**: `S`=서비스 단위(Mockito) · `C`=컨트롤러 슬라이스(MockMvc) · `R`=리포지토리(@DataJpaTest) · `I`=통합(@SpringBootTest)
- **기대 결과**의 `ErrorCode.XXX`는 `BusinessException` 발생을 의미하며, HTTP 매핑은 [00-test-strategy.md](00-test-strategy.md) §예외 매핑 참고.
- ⚠️ **알려진 갭**: 현재 구현이 의도와 다를 수 있는 동작. "현재 동작을 고정하는 테스트 + `// KNOWN GAP` 주석" 방침(전략 문서 §9).
