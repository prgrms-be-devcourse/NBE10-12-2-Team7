# 핵심 동작 흐름 (시퀀스)

런타임에 요청이 레이어를 **시간순**으로 어떻게 흐르는지 본다. 정적 구조는 [03-application.md](03-application.md), 실패 응답은 공통 `ErrorResponse` JSON으로 통일된다.

---

## 로그인 — `POST /api/auth/login`

```mermaid
sequenceDiagram
    actor C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant MR as MemberRepository
    participant PE as PasswordEncoder
    participant TP as JwtTokenProvider

    Note over C,AC: /api/auth/login 은 permitAll · @Valid 형식 오류 시 400 INVALID_INPUT_VALUE
    C->>AC: POST /api/auth/login {email, password}
    AC->>AS: login(request)
    AS->>MR: findByEmail(email)
    alt 회원 없음
        MR-->>AS: empty
        AS-->>C: 404 MEMBER_NOT_FOUND
    else 회원 존재
        MR-->>AS: Member
        alt status = DELETED
            AS-->>C: 400 DELETED_MEMBER
        else status = SUSPENDED
            AS-->>C: 403 SUSPENDED_MEMBER
        else status = ACTIVE
            AS->>PE: matches(raw, encoded)
            alt 비밀번호 불일치
                PE-->>AS: false
                AS-->>C: 401 INVALID_PASSWORD
            else 일치
                PE-->>AS: true
                AS->>TP: createAccessToken(memberId, role)
                TP-->>AS: JWT
                AS-->>AC: LoginResponse(accessToken)
                AC-->>C: 200 OK {accessToken}
            end
        end
    end
    Note over AS,C: 모든 실패는 BusinessException → GlobalExceptionHandler가 공통 ErrorResponse 로 변환
```

### 메모
- `/api/auth/login`·`/signup`은 `permitAll` → 토큰 없이 호출한다. **회원가입은 토큰을 발급하지 않는다**(201만 반환).
- 인증 주체는 `Long memberId`: 토큰 `subject`=memberId, claim=role. 발급된 토큰 검증 시 DB 재조회가 없다(`UserDetailsService` 미사용).
- 따라서 **상태(`status`)·권한(`role`) 변경은 토큰 만료(기본 1시간) 전까지 반영되지 않는다** — 로그인 시점에 확정.

---

## 상품 등록 — `POST /api/products`

```mermaid
sequenceDiagram
    actor C as Client
    participant F as JwtAuthenticationFilter
    participant PC as ProductController
    participant PS as ProductService
    participant MR as MemberRepository
    participant CR as CategoryRepository
    participant PR as ProductRepository

    Note over C,F: 인증 필요 · 토큰 없음/무효 시 401 UNAUTHORIZED (EntryPoint, 컨트롤러 도달 전)
    C->>F: POST /api/products (Bearer 토큰) {categoryId, title, price, ...}
    F->>PC: 인증 등록 → principal = memberId
    PC->>PS: createProduct(memberId, request)
    PS->>PS: validateRequest(title, price)
    alt title 공백
        PS-->>C: 400 INVALID_PRODUCT_TITLE
    else price null/음수
        PS-->>C: 400 INVALID_PRODUCT_PRICE
    else 검증 통과
        PS->>MR: findById(memberId)
        alt 회원 없음
            MR-->>PS: empty
            PS-->>C: 404 MEMBER_NOT_FOUND
        else 회원 존재
            MR-->>PS: Member
            PS->>CR: findById(categoryId)
            alt 카테고리 없음
                CR-->>PS: empty
                PS-->>C: 404 CATEGORY_NOT_FOUND
            else 카테고리 존재
                CR-->>PS: Category
                PS->>PR: save(Product.create(...))
                PR-->>PS: savedProduct
                PS-->>PC: ProductResponse
                PC-->>C: 201 Created {product}
            end
        end
    end
    Note over PS,C: 실패는 BusinessException → GlobalExceptionHandler → 공통 ErrorResponse
```

### 메모
- 인증 필요. `@AuthenticationPrincipal Long memberId`로 작성자를 식별한다.
- 입력 검증은 컨트롤러 `@Valid`가 아니라 **서비스에서 수동 검증**(`validateProductFields`): 제목 공백 → 400, 가격 `null`/음수 → 400. (로그인과 달리 컨트롤러에 `@Valid`가 없다.)
- `member`·`category`는 조회 실패 시 각각 404. 저장 성공 시 `201 Created`.
- 트랜잭션: 쓰기 메서드는 `@Transactional`, 조회는 클래스 기본 `readOnly=true`. 신규 `Product`는 `ON_SALE`·`viewCount=0`·`hidden=false`로 생성된다([04-erd.md](04-erd.md)).
