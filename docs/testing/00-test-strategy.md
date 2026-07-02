# 00. 테스트 공통 전략

도메인별 케이스 표를 보기 전에 **반드시 합의해야 할 공통 규칙**입니다.
이게 없으면 사람마다 테스트 스타일·픽스처가 제각각이 되고, 같은 케이스를 여러 계층에서 중복 검증하게 됩니다.

---

## 1. 테스트 계층 전략 (어느 로직을 어디서 검증하나)

같은 규칙을 여러 계층에서 중복 검증하면 낭비입니다. **계층별 책임을 아래로 고정**합니다.

| 계층 | 표기 | 도구 | 검증 대상 | 검증하지 않는 것 |
|---|---|---|---|---|
| 서비스 단위 | `S` | JUnit5 + Mockito (`@ExtendWith(MockitoExtension.class)`) | **비즈니스 규칙·예외 분기·검증 순서**. 리포지토리는 mock | HTTP, 실제 쿼리, 보안 |
| 컨트롤러 슬라이스 | `C` | `@WebMvcTest` + `MockMvc` + `@MockBean` 서비스 + spring-security-test | 요청/응답 매핑, HTTP 상태, `@Valid` 검증, **인가(인증 필요 여부·역할)** | 비즈니스 로직(서비스는 mock) |
| 리포지토리 | `R` | `@DataJpaTest` + H2 + `TestEntityManager` | **커스텀 쿼리 메서드**, soft-delete/hidden 필터, 정렬, UNIQUE 제약 | 서비스 로직 |
| 통합 | `I` | `@SpringBootTest` + `MockMvc` | 실제 JWT 필터까지 엮인 보안 정책, 핵심 happy-path 1~2개 | 모든 분기(단위에서 이미 검증) |

**원칙**
- 비즈니스 분기(예: "거래완료 상품은 수정 불가")는 **서비스 단위(S)에서만** 빠짐없이. 컨트롤러(C)에서는 같은 분기를 반복하지 말 것.
- 컨트롤러(C)는 "올바른 인자로 서비스를 호출하는가 + 응답/상태/인가" 만. `verify(service).method(...)` 중심.
- `@Valid` Bean Validation은 **컨트롤러(C)에서만** 검증 가능(서비스 단위에서는 안 걸림). §4 참고.
- 리포지토리(R)는 **커스텀 메서드명이 의도대로 동작하는지** 만. 기본 `save/findById` 같은 Spring Data 기본 기능은 테스트하지 않음.

> 보안 전역 정책(URL별 인증/인가)은 이미 `SecurityPolicyTest`(통합)가 존재합니다. 도메인 문서의 "인가" 행은 **컨트롤러 슬라이스(C)** 기준으로 자기 도메인 URL만 확인하면 됩니다.

---

## 2. 픽스처(객체 생성) 전략 — ⚠️ 가장 먼저 합의

엔티티는 **`private` 생성자 + 정적 팩토리**이고 **세터가 없으며 `id`는 DB가 생성**합니다.
(`Member.createUser(...)`, `Product.create(...)`, `Comment.of(...)`, `Favorite.of(...)`, `Report.ofProduct/ofMember(...)`)

→ 서비스 단위 테스트(S)에서 "id가 5인 회원", "특정 회원이 소유한 상품" 같은 객체가 필요한데, 세터·id 주입 수단이 없습니다. **세 가지 중 1개로 팀 통일**:

| 방법 | 방식 | 권장 상황 |
|---|---|---|
| (A) 리플렉션 헬퍼 | `ReflectionTestUtils.setField(member, "id", 1L)` | **서비스 단위(S)** — 가장 단순. 권장 |
| (B) 테스트 빌더/ObjectMother | `test` 소스에 `MemberFixture.user(1L)` 류 헬퍼 클래스 | 같은 객체가 여러 테스트에서 반복될 때 |
| (C) TestEntityManager persist | `em.persistAndFlush(...)` 로 실제 id 부여 | **리포지토리(R)** — 실제 DB 필요할 때 |

**권장**: 공용 `src/test/java/.../support/` 아래에 `MemberFixture`, `ProductFixture` 등 ObjectMother를 두고, 내부에서 (A) 리플렉션으로 id·연관관계를 세팅. 모든 도메인이 같은 픽스처를 재사용.

```java
// 예시: support/ProductFixture.java
public final class ProductFixture {
    public static Product onSale(Long id, Member owner, Category category) {
        Product p = Product.create(owner, category, "제목", "설명", 10000, "서울 강남구");
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }
    public static Product completed(Long id, Member owner, Category category) {
        Product p = onSale(id, owner, category);
        p.complete();
        return p;
    }
}
```

> `deletedAt`/`hidden`/`tradeStatus`도 세터가 없으므로, 필요한 상태는 **공개 메서드**(`softDelete()`, `hide()`, `complete()`, `changeTradeStatus()`)로 만들거나 리플렉션으로 세팅합니다.

---

## 3. H2(테스트) vs MySQL(운영) 차이 — 리포지토리(R) 테스트 주의

테스트는 H2 in-memory, 운영은 MySQL입니다. 아래는 **결과가 달라질 수 있는 지점**:

- **검색 대소문자**: `ProductSpecification`는 `lower(title) LIKE lower(%keyword%)`로 **양쪽 모두 소문자화** → 영문 대소문자 무시. → "ABC"로 등록, "abc"로 검색 시 매칭되는 케이스를 꼭 넣을 것.
- **한글 LIKE/정렬**: H2와 MySQL의 콜레이션이 달라 한글 부분일치·정렬 순서가 미묘하게 다를 수 있음. 정렬 검증은 **id 기준 tiebreaker가 있는 finder**(예: 관심목록 `...OrderByCreatedAtDescIdDesc`)를 신뢰하고, `createdAt`만으로 순서를 단정하지 말 것(동일 시각 가능).
- **soft-delete/hidden 필터**: `deletedAt IS NULL`, `hidden = false` 조건이 쿼리에 실제로 걸리는지가 R 테스트의 핵심. 삭제/숨김 데이터를 섞어 넣고 **제외되는지** 확인.
- **UNIQUE 제약**: `favorites(member_id, product_id)` 유니크. H2에서도 동일하게 위반 시 `DataIntegrityViolationException` 발생하는지 확인.
- **`@DataJpaTest`는 `ddl-auto`와 무관하게 스키마를 생성**하고 테스트마다 롤백됨. 운영 `ddl-auto: update`와 별개.

---

## 4. 검증(Validation) — `@Valid`가 걸린 곳 / 서비스에서 막는 곳

**중요**: 도메인마다 검증 위치가 다릅니다. 헷갈리면 엉뚱한 계층에서 테스트하게 됩니다.

| 요청 DTO | `@Valid` 컨트롤러 | 검증 위치 | 비고 |
|---|---|---|---|
| SignupRequest, LoginRequest | ✅ | 컨트롤러(C) | email/password/nickname 형식·길이 |
| MemberUpdateRequest | ✅ | 컨트롤러(C) | nickname 2~20자 |
| CommentCreate/UpdateRequest | ✅ | 컨트롤러(C) | content 필수·500자 |
| ProductReport/MemberReportCreateRequest | ✅ | 컨트롤러(C) | reason 필수, content ≤500(널 허용) |
| **ProductCreate/UpdateRequest** | ❌ **없음** | **서비스(S)** | `validateProductFields`: title 공백→`INVALID_PRODUCT_TITLE`, price null·음수→`INVALID_PRODUCT_PRICE` |
| **ProductStatusUpdateRequest** | ❌ 없음 | 서비스(S) | `parseTradeStatus`: null/공백/잘못된 값→`INVALID_TRADE_STATUS` |
| **AdminMember/ReportStatusUpdateRequest** | ❌ 없음 | 서비스(S) | `parseStatus`: null/공백/잘못된 값→`INVALID_*_STATUS`, **`trim()` 후 파싱** |
| ProductSearch(쿼리파라미터) | ❌ 없음 | 서비스(S) | 음수가격·minPrice>maxPrice→`INVALID_SEARCH_CONDITION` |

- `@Valid` 실패 → `MethodArgumentNotValidException` → **400 / `COMMON_002` / 첫 번째 필드 메시지**. (C 테스트에서 `jsonPath`로 메시지 확인 가능하나, **첫 필드**만 노출되므로 메시지 단정은 느슨하게.)
- 서비스 검증 실패 → `BusinessException(ErrorCode)` → 해당 status. (S 테스트에서 `assertThatThrownBy(...).isInstanceOf(BusinessException.class)` + ErrorCode 비교.)

---

## 5. 인증 주체(principal)와 슬라이스 테스트 셋업

- 모든 컨트롤러는 `@AuthenticationPrincipal Long memberId`로 **principal = 회원 id(Long)** 를 받습니다(JWT subject).
- 인가 정책(`SecurityConfig`): 비공개 URL은 `anyRequest().authenticated()`, `/api/admin/**`는 `hasRole("ADMIN")`.
- **컨트롤러 슬라이스(C) 셋업 시**:
  - 인증 사용자: `post(...).with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))))` 처럼 **principal을 Long으로** 넣어야 `@AuthenticationPrincipal Long memberId`가 채워짐. (`@WithMockUser`는 principal이 String이라 그대로 쓰면 id 주입이 안 됨 — 주의)
  - 관리자: authority `ROLE_ADMIN`.
  - 비인증: 아무 인증 없이 호출 → **401**(EntryPoint). 권한 부족(USER가 admin URL) → **403**(AccessDeniedHandler).
  - `@WebMvcTest`는 `SecurityConfig`·`JwtAuthenticationFilter`·`JwtTokenProvider`를 자동 포함하지 않으므로 `@Import` 하거나, 필터를 제외하고 `@WithMockUser`/`SecurityMockMvcRequestPostProcessors`로 대체. **팀에서 한 방식으로 통일**(권장: 공용 `@Import(SecurityConfig.class)` + 위 RequestPostProcessor).

---

## 6. JWT 필터 동작 (인증 관련 테스트 시 사실관계)

`JwtAuthenticationFilter`는:
- 토큰 없음 → 그냥 통과(인증 미설정) → 비공개 URL은 401.
- 토큰 깨짐/만료 → 예외를 잡고 `authError` 속성만 세팅, **요청은 통과** → 인증 미설정 → 401.
- 토큰 정상 → principal=memberId, authority=role 로 인증 설정.
- ⚠️ **필터는 DB의 회원 상태(SUSPENDED/DELETED)를 확인하지 않음** → 유효한 토큰을 가진 정지/탈퇴 회원도 필터는 통과. member 도메인 엔드포인트만 서비스에서 `validateActiveMember`로 막고, **product/comment/favorite/report는 회원 상태를 재확인하지 않음**(아래 §8 갭 N2).

---

## 7. 예외 → HTTP 응답 매핑 (GlobalExceptionHandler)

| 발생 예외 | HTTP | 응답 코드 | 메시지 |
|---|---|---|---|
| `BusinessException(ec)` | `ec.status` | `ec.code` | `ec.message` |
| `MethodArgumentNotValidException`(@Valid) | 400 | `COMMON_002` | **첫 번째** 필드 에러 메시지 |
| `AccessDeniedException` | 403 | `COMMON_004` | 접근 권한이 없습니다. |
| 그 외 `Exception` | 500 | `COMMON_001` | 서버 내부 오류 |

응답 바디는 공용 `ErrorResponse` 포맷. C 테스트에서 `status().is(...)` + `jsonPath("$.code")` 등으로 확인. ErrorCode 카탈로그는 `global/exception/ErrorCode.java` 참조.

---

## 8. 알려진 갭(Known Gaps) — 테스트로 "현재 동작"을 고정할 것

아래는 구현이 의도와 다를 수 있는 지점입니다. **지금은 현재 동작을 고정하는 테스트 + `// KNOWN GAP: ...` 주석**으로 남기고, 기대 동작 테스트는 `@Disabled("기대동작 - 미구현")`로 별도 표기. (수정 여부는 팀장 결정)

| 코드 | 위치 | 현재 동작 | 기대(의심) |
|---|---|---|---|
| **N1** | AdminMemberService | 관리자가 **다른 관리자/시드 관리자/본인**을 SUSPENDED·DELETED 가능(가드 없음) | 관리자 보호 필요 |
| **N2** | JwtAuthenticationFilter | 정지/탈퇴 회원도 **기존 발급 토큰으로 product/comment/favorite/report 접근 가능**(상태 재검증 없음) | 토큰 무효화/상태 재검증 |
| **N3** | AdminDashboardService | `pendingReports` = `RECEIVED`만 집계 (REVIEWING 제외) | 처리 대기 = RECEIVED+REVIEWING? |
| 부수 | AdminDashboardService | 총 회원·상품 수에 **soft-deleted/hidden 포함**(`count()` 전체) | 활성만? |
| 부수 | report/favorite/comment | **삭제·숨김 상품에도** 신고/관심/댓글 가능(`existsById`/`findById`는 soft-delete 무시) | 접근 차단? |

---

## 9. 컨벤션 & DoD

- **명명**: `@DisplayName("한글 시나리오")` + 메서드명 `대상_조건_결과()` (예: `login_정지회원_SUSPENDED예외()`).
- **구조**: 메서드별 `@Nested` 그룹(예: `@Nested class 로그인`). given/when/then 주석 또는 빈 줄 구분.
- **단언**: 예외는 AssertJ `assertThatThrownBy(...).isInstanceOf(BusinessException.class)` 후 `getErrorCode()` 비교(헬퍼 권장). 상태 변화는 엔티티 getter로 확인.
- **상호작용**: 컨트롤러(C)는 `verify(service).xxx(eq(...))`, 부작용 없는 호출엔 `verifyNoInteractions`.
- **DoD(완료 기준)**: 도메인 표의 모든 행에 대응 테스트 존재 + 모든 예외 분기 1개 이상 + happy-path 1개 이상. 서비스 계층 라인/브랜치 커버리지 목표는 팀 합의(권장 80%+).
- **데이터 격리**: 통합/리포지토리 테스트는 트랜잭션 롤백 기본. 시드(`AdminAccountInitializer`)는 `@Profile("!test")`라 test 프로파일에서 비활성 — 통합 테스트에서 관리자 필요 시 직접 생성.
