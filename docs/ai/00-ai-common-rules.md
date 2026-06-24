# 00 · AI 공통 규칙 (AI Agent Common Rules)

> **이 문서는 모든 팀원이 어떤 AI 에이전트를 쓰든 가장 먼저 학습시키는 단일 기준 문서다.**
> Claude, Gemini, ChatGPT 등 도구가 달라도 이 문서를 읽히면 프로젝트 이해·역할·개발 흐름이 같아진다.

---

## 이 환경은 어떻게 동작하는가 (3단계)

```
1단계  공통 이해   → 이 문서(00)로 프로젝트 전체 맥락을 학습시킨다.
2단계  역할 적용   → "어떤 역할?"을 선택하면 해당 역할 문서(01~05)로 넘어가 그 도메인만 개발한다.
3단계  개발 흐름   → 모든 기능을 아래 5단계 순서로 동일하게 진행한다.
```

팀원이 서로 다른 에이전트를 써도 이 3단계 덕분에 **프로젝트 이해·개발 범위·개발 방식**이 일관되게 유지된다.

---

## 핵심 개발 흐름 (모든 기능 공통, 5단계)

```
① ErrorCode 작성  →  ② 기능 구현  →  ③ 테스트 코드 작성  →  ④ Postman 테스트  →  ⑤ PR 요청
```

- **① ErrorCode 작성**: 바로 코드를 짜지 않는다. 먼저 발생 가능한 예외 상황을 분석하고, 필요한 ErrorCode를 자기 도메인 영역에 정의한다.
- **② 기능 구현**: 계층 규칙(Controller·Service·Repository·Entity·DTO)에 맞춰 구현한다.
- **③ 테스트 코드 작성**: 단위 테스트(성공 1 + 주요 실패 2)를 작성한다.
- **④ Postman 테스트**: 실제 요청으로 성공/실패 케이스를 검증하고 시나리오 문서를 남긴다.
- **⑤ PR 요청**: PR 체크리스트를 채워 `feature/*` → `develop`으로 올린다.

이 5단계는 **초기 공통 기준**이다. 각 팀원은 자신의 도메인·상황에 맞게 통합 테스트·성능 테스트·로그·API 문서화 등을 **추가로 확장**해도 좋다. 줄이지는 않는다.

### 작업은 작게 쪼개서 진행한다 (PR 단위 규칙)

기능을 한 번에 다 구현하지 않는다. 리뷰어가 빠르게 검토할 수 있도록 **요청받은 기능을 작은 단위로 쪼개서** 진행한다.

1. 구현을 시작하기 전에, 기능을 **리뷰 가능한 작은 작업 단위로 분할**하고 그 분할 계획(작업 단위 목록과 순서)을 먼저 사용자에게 제시한다.
2. 사용자가 승인하면 **한 단위씩** 5단계(ErrorCode → 구현 → 테스트 → Postman → PR)를 거쳐 **작은 PR 하나**로 올린다.
3. **한 PR에 여러 작업 단위를 섞지 않는다.** 다음 단위는 별도 PR로 진행한다.

쪼개는 기준(예): 엔티티·DTO 골격 / 단일 엔드포인트 / 검증·예외 처리 / 권한 검증 — 각각 독립적으로 리뷰·머지될 수 있는 크기로 나눈다.

---

## 0. 시작 — 역할 선택 (이 문서를 읽은 직후 가장 먼저 할 일)

이 공통 규칙을 모두 읽었다면, **코드 작업을 시작하기 전에 사용자에게 가장 먼저 다음을 질문한다.**

> "이 프로젝트에서 어떤 역할을 맡으셨나요? 아래에서 골라 주세요."
> 1. 팀장 / 공통구조 · Admin
> 2. Auth · Member
> 3. Product · Category · Trade · Search
> 4. Favorite · Comment
> 5. Report

사용자가 역할을 선택하면, 아래 매핑에 따라 **해당 문서를 읽고 그 문서의 지시에 따라 즉시 작업을 시작한다.**

| 선택 | 역할 | 읽을 문서 |
| --- | --- | --- |
| 1 | 팀장 / 공통구조 · Admin | 05-admin-common-agent.md |
| 2 | Auth · Member | 01-auth-member-agent.md |
| 3 | Product · Category · Trade · Search | 02-product-category-agent.md |
| 4 | Favorite · Comment | 03-favorite-comment-agent.md |
| 5 | Report | 04-report-agent.md |

통합/머지는 팀장이 09:00~17:00 동안 PR이 올라올 때마다 별도로 보유한 통합 도구로 상시 검수·통합한다(팀원 배포 폴더에는 포함되지 않는다).

---

## 1. 프로젝트 개요

- 프로젝트명: 동네마켓 API
- 유형: 지역 기반 중고거래 플랫폼 REST API 서비스
- 기술 스택: Java, Spring Boot, Spring Data JPA, Spring Security, JWT, MySQL, Swagger(Springdoc), Gradle, JUnit5, Mockito, Postman

---

## 2. 전체 도메인 구조

```
global  auth  member  product  category  favorite  comment  report  admin  trade  search
```

각 도메인 기본 구조:
```
domain
├── controller
├── service
├── repository
├── entity
└── dto
```

global 공통 구조(팀장 관리):
```
global
├── common
├── config
├── exception
├── response
└── security
```

---

## 3. 절대 규칙

```
1. 담당 도메인 외 패키지를 임의로 수정하지 않는다.
2. global 구조와 COMMON ErrorCode 영역은 팀장만 수정한다.
3. ErrorCode는 자기 담당 도메인 주석 영역에만 추가한다.
4. Entity 연관관계를 임의로 변경하지 않는다.
5. Controller에 비즈니스 로직을 두지 않고 Service에 작성한다.
6. Repository에는 DB 접근 로직만 작성한다.
7. Entity를 API 응답으로 직접 반환하지 않고 Request/Response DTO를 분리한다.
8. 성공 응답은 ApiResponse<T>, 예외는 BusinessException + ErrorCode를 사용한다(RuntimeException 직접 던지기 금지).
9. 인증이 필요한 API는 현재 로그인 사용자를 사용하고, 권한 검증이 필요한 API는 반드시 검증한다.
10. Validation을 적용한다.
11. 테스트 코드 · Postman 시나리오 없이 기능을 완료로 처리하지 않는다.
```

---

## 4. ErrorCode 규칙

- ErrorCode.java의 **자기 도메인 주석 영역 안에만** 추가한다. COMMON 영역은 팀장만 수정.
- 이름은 대문자 스네이크 케이스, code는 `도메인_번호`(예: `AUTH_001`), message는 한글 문장.

도메인별 Prefix: `COMMON / AUTH / MEMBER / PRODUCT / CATEGORY / FAVORITE / COMMENT / REPORT / TRADE / ADMIN / SEARCH`

팀장이 초기에 만들어두는 ErrorCode.java 골격 (도메인별 주석 영역):
```java
public enum ErrorCode {

    // ===== COMMON ERROR (팀장만 수정) =====
    INTERNAL_SERVER_ERROR(500, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(400, "COMMON_002", "잘못된 입력값입니다."),
    UNAUTHORIZED(401, "COMMON_003", "인증이 필요합니다."),
    FORBIDDEN(403, "COMMON_004", "접근 권한이 없습니다."),

    // ===== AUTH ERROR (김대연) =====
    DUPLICATE_EMAIL(409, "AUTH_001", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(409, "AUTH_002", "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(401, "AUTH_003", "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(401, "AUTH_004", "유효하지 않은 토큰입니다."),

    // ===== MEMBER ERROR (김대연) =====
    MEMBER_NOT_FOUND(404, "MEMBER_001", "회원을 찾을 수 없습니다."),
    DELETED_MEMBER(400, "MEMBER_002", "탈퇴한 회원입니다."),
    SUSPENDED_MEMBER(403, "MEMBER_003", "정지된 회원입니다."),

    // ===== PRODUCT ERROR (한상민) =====
    PRODUCT_NOT_FOUND(404, "PRODUCT_001", "상품을 찾을 수 없습니다."),
    PRODUCT_OWNER_ONLY(403, "PRODUCT_002", "상품 작성자만 처리할 수 있습니다."),
    HIDDEN_PRODUCT(403, "PRODUCT_003", "숨김 처리된 상품입니다."),
    DELETED_PRODUCT(404, "PRODUCT_004", "삭제된 상품입니다."),

    // ===== CATEGORY ERROR (한상민) =====
    CATEGORY_NOT_FOUND(404, "CATEGORY_001", "카테고리를 찾을 수 없습니다."),

    // ===== TRADE ERROR (한상민) =====
    INVALID_TRADE_STATUS(400, "TRADE_001", "잘못된 거래 상태입니다."),
    CANNOT_CHANGE_COMPLETED_PRODUCT(400, "TRADE_002", "거래완료된 상품은 상태를 변경할 수 없습니다."),

    // ===== SEARCH ERROR (한상민) =====
    INVALID_SEARCH_CONDITION(400, "SEARCH_001", "잘못된 검색 조건입니다."),

    // ===== FAVORITE ERROR (권건우) =====
    FAVORITE_ALREADY_EXISTS(409, "FAVORITE_001", "이미 관심 등록한 상품입니다."),
    FAVORITE_NOT_FOUND(404, "FAVORITE_002", "관심 상품을 찾을 수 없습니다."),

    // ===== COMMENT ERROR (권건우) =====
    COMMENT_NOT_FOUND(404, "COMMENT_001", "댓글을 찾을 수 없습니다."),
    COMMENT_OWNER_ONLY(403, "COMMENT_002", "댓글 작성자만 처리할 수 있습니다."),

    // ===== REPORT ERROR (서유진) =====
    REPORT_NOT_FOUND(404, "REPORT_001", "신고 내역을 찾을 수 없습니다."),
    CANNOT_REPORT_OWN_PRODUCT(400, "REPORT_002", "본인이 등록한 상품은 신고할 수 없습니다."),
    CANNOT_REPORT_SELF(400, "REPORT_003", "본인 계정은 신고할 수 없습니다."),
    INVALID_REPORT_TARGET(400, "REPORT_004", "잘못된 신고 대상입니다."),

    // ===== ADMIN ERROR (팀장) =====
    ADMIN_ONLY(403, "ADMIN_001", "관리자만 접근할 수 있습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
```

---

## 5. 공통 응답 규칙

성공:
```json
{ "status": 200, "message": "요청이 성공적으로 처리되었습니다.", "data": {} }
```
에러:
```json
{ "status": 404, "error": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다.", "timestamp": "2026-06-17T12:00:00" }
```

---

## 6. 계층별 책임

- **Controller**: Request DTO 수신, @Valid, Service 호출, ApiResponse 반환, Swagger. (비즈니스 로직·Repository 직접 호출·Entity 반환 금지)
- **Service**: 비즈니스 로직, 권한·중복·상태 검증, BusinessException, 트랜잭션, DTO 변환. (HTTP 객체 직접 처리 금지)
- **Repository**: DB 접근만.
- **Entity**: 필드·연관관계·상태 변경 메서드. (API 응답/요청 객체 역할 금지)
- **DTO**: Request/Response 분리, Entity 직접 노출 금지.

---

## 7. 인증 / 인가

- JWT Access Token 기반, 헤더 `Authorization: Bearer {accessToken}`.
- 인증 불필요: 회원가입, 로그인, 상품 목록/상세 조회, 카테고리 조회, 댓글 목록 조회.
- 그 외 쓰기·내 정보·관리자 API는 인증 필요.
- 본인 검증: 상품 수정/삭제/거래상태 변경(작성자), 댓글 수정/삭제(작성자), 관심 취소(등록자). 관리자 API는 ROLE_ADMIN.

---

## 8. 도메인별 규칙 (ERD 기준 — 정본)

### Auth / Member
```
이메일은 중복 가입할 수 없다 (members.email UNIQUE).
닉네임은 중복될 수 없다 (members.nickname UNIQUE).
비밀번호는 BCrypt로 암호화한다.
회원 탈퇴는 status=DELETED + deleted_at 소프트 삭제.
탈퇴/정지(SUSPENDED) 회원은 로그인할 수 없다.
Role: ROLE_USER, ROLE_ADMIN  /  MemberStatus: ACTIVE, SUSPENDED, DELETED
```

### Product / Category / Trade / Search
```
상품 등록은 로그인 사용자만. 수정/삭제/거래상태 변경은 작성자 본인만.
삭제(deleted_at)·숨김(hidden) 상품은 일반 목록에서 제외.
상세 조회 시 view_count 1 증가. 등록 시 존재하는 카테고리만 사용.
TradeStatus: ON_SALE, RESERVED, COMPLETED  (COMPLETED는 되돌릴 수 없다)
검색/필터: keyword, categoryId, region, tradeStatus. 잘못된 조건은 SEARCH ErrorCode.
```

### Favorite / Comment
```
관심 등록은 로그인 사용자만. favorites(member_id, product_id) UNIQUE.
중복 관심 등록 409, 없는 관심 취소 404.
댓글 작성은 로그인만, 목록 조회는 비로그인 허용. 수정/삭제는 작성자 본인만.
댓글 삭제는 소프트 삭제(deleted_at).
```

### Report
```
로그인 사용자만 신고. 상품 신고=target_product_id, 사용자 신고=target_member_id (둘 중 하나만).
신고 생성 시 기본 상태 RECEIVED. 본인 상품/본인 계정은 신고 불가.
ReportType: PRODUCT, MEMBER
ReportReason: FAKE_ITEM, FRAUD_SUSPECTED, PROHIBITED_ITEM, INAPPROPRIATE_CONTENT, ETC
ReportStatus: RECEIVED, REVIEWING, COMPLETED, REJECTED  (상태 변경은 admin 영역)
```

### Admin (팀장)
```
/api/admin/** 경로, ROLE_ADMIN만 접근. 관리자 계정은 초기 데이터로 생성.
관리자는 작성자가 아니어도 상품 숨김(hidden), 댓글 소프트 삭제 가능.
회원 상태(ACTIVE/SUSPENDED/DELETED) 변경 가능.
```

---

## 9. API URI

```
# Auth        POST /api/auth/signup, POST /api/auth/login
# Member      GET|PATCH|DELETE /api/members/me
# Product     POST|GET /api/products, GET|PATCH|DELETE /api/products/{productId},
#             PATCH /api/products/{productId}/status, GET /api/members/me/products
# Category    GET /api/categories, GET /api/categories/{categoryId}/products
# Favorite    POST|DELETE /api/products/{productId}/favorites, GET /api/members/me/favorites
# Comment     POST|GET /api/products/{productId}/comments, PATCH|DELETE /api/comments/{commentId}
# Report      POST /api/products/{productId}/reports, POST /api/members/{memberId}/reports,
#             GET /api/members/me/reports
# Admin       GET /api/admin/members, GET /api/admin/members/{memberId},
#             PATCH /api/admin/members/{memberId}/status,
#             GET /api/admin/products, GET /api/admin/products/{productId},
#             PATCH /api/admin/products/{productId}/hidden, DELETE /api/admin/products/{productId},
#             GET /api/admin/reports, GET /api/admin/reports/{reportId},
#             PATCH /api/admin/reports/{reportId}/status,
#             GET /api/admin/comments, DELETE /api/admin/comments/{commentId},
#             GET /api/admin/dashboard
```

---

## 10. 기능 완료 기준

5단계 흐름(① ErrorCode → ② 구현 → ③ 테스트 → ④ Postman → ⑤ PR)을 모두 마치고, 아래가 갖춰지면 완료로 본다.
```
Entity / Request DTO / Response DTO / Repository / Service / Controller
Validation / ApiResponse / BusinessException / Swagger 문서
단위 테스트(성공1+실패2) / Postman 시나리오 문서 / PR 체크리스트
```
> 참고: Swagger 문서화는 완료 기준에 포함된다(헤드라인 5단계의 "② 기능 구현" 단계에서 함께 작성).

---

## 11. PR 체크리스트

```markdown
## 작업 내용 / 담당 도메인 / 변경 파일

## AI 사용 여부
- [ ] AI 에이전트를 사용했고, 생성 코드를 직접 검토했다.
- [ ] 담당 패키지 외 파일을 수정하지 않았다.

## 5단계 흐름
- [ ] ① ErrorCode 작성(예외 분석 포함)
- [ ] ② 기능 구현
- [ ] ③ 단위 테스트 작성
- [ ] ④ Postman 테스트
- [ ] ⑤ PR 요청 준비(Swagger 포함)
- [ ] 이 PR은 하나의 작은 작업 단위만 담았다 (여러 기능 섞지 않음)

## 검증
- [ ] 빌드 성공 / 서버 정상 기동 / 공통 응답 형식 준수
```

---

## 12. 금지 사항

```
global 공통 구조·SecurityConfig·공통 응답/에러 구조 변경
다른 팀원 도메인 패키지 수정 / Entity 연관관계 임의 변경 / API URI 임의 변경
COMMON ErrorCode 영역 수정
테스트·Postman 없이 완료 처리
```

---

## 최종 원칙

AI 에이전트는 빠른 코드 생성 도구가 아니라, **팀의 공통 아키텍처와 5단계 개발 흐름을 지키며 기능 구현을 보조하는 도구**다. 모든 기능은 `ErrorCode → 구현 → 테스트 → Postman → PR` 순서를 지킨다.
