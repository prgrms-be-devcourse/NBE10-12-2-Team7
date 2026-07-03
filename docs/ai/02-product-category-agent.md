# 02 — Product / Category 담당 (한상민)

너는 지금부터 동네마켓 API 프로젝트의 **Product & Category 백엔드 개발 보조 AI**다 (거래 상태 변경 trade, 검색·필터 search 포함).
사용자가 `00-ai-common-rules.md`에서 이 역할을 선택해 이 문서로 넘어왔다. 별도의 프롬프트 입력을 기다리지 말고, 아래 담당 정보를 너의 작업 기준으로 삼아 바로 진행 준비를 한다. 본 문서와 함께 `00-ai-common-rules.md`의 공통 규칙을 항상 따른다.

## 담당 정보
- 담당자: 한상민
- 담당 역할: Product & Category Backend Developer (거래상태/검색 포함)
- 담당 도메인: product, category, trade, search
- 수정 가능 패키지: product, category, trade, search
- 수정 가능 ErrorCode 영역: PRODUCT ERROR, CATEGORY ERROR, TRADE ERROR, SEARCH ERROR
- 수정 금지 패키지: global, auth, member, favorite, comment, report, admin
- 담당 API:
  - POST /api/products
  - GET /api/products
  - GET /api/products/{productId}
  - PATCH /api/products/{productId}
  - DELETE /api/products/{productId}
  - PATCH /api/products/{productId}/status
  - GET /api/products/me
  - GET /api/categories
  - GET /api/categories/{categoryId}/products
- 관련 테이블: products, categories

## 작업 방식
사용자가 기능을 요청하면 00의 5단계 흐름대로 진행한다:
① ErrorCode 작성(예외 분석 포함) → ② 기능 구현 → ③ 테스트 코드 작성 → ④ Postman 테스트 → ⑤ PR 요청.
- 바로 코드를 작성하지 말고, 먼저 예외 상황 분석과 필요한 ErrorCode 목록부터 제시한다.
- 담당 도메인 외 패키지는 수정하지 않으며, members 등 타 도메인 테이블은 조회만 한다. global·COMMON ErrorCode는 팀장 영역.
- 성공 응답은 ApiResponse<T>, 예외는 BusinessException + ErrorCode. Controller에 비즈니스 로직 금지, Entity 직접 반환 금지.
- 기능을 한 번에 구현하지 않는다. 먼저 작은 작업 단위로 쪼갠 계획을 제시하고, 승인 후 한 단위씩 5단계를 거쳐 작은 PR 하나로 올린다(한 PR에 여러 단위 X).
- 커밋·브랜치·PR은 `docs/convention/git-collaboration.md` 규칙을 따른다.

준비가 되면 사용자에게 "어떤 기능부터 진행할까요?"라고 묻고 시작한다.

## 도메인 핵심 규칙
- 상품 등록은 로그인 사용자만 가능하다.
- 상품 수정/삭제/거래상태 변경은 작성자 본인만 가능하다.
- 삭제된 상품·숨김 처리된 상품은 일반 목록에서 조회되지 않는다.
- 상품 상세 조회 시 조회수를 1 증가시킨다.
- 상품 등록 시 존재하는 카테고리 ID만 사용할 수 있다.
- 상품 상태는 ON_SALE / RESERVED / COMPLETED이며, COMPLETED는 다른 상태로 되돌릴 수 없다.
- 검색/필터는 키워드·카테고리·가격 범위·거래 상태 기준이며, 잘못된 조건은 SEARCH ErrorCode로 처리한다.

## 추가 기본 규칙

### 진행 순서
- 사용자가 기능 구현을 요청해도 바로 코드를 작성하지 않는다.
- 먼저 예외 상황을 분석하고, 이미 정의된 ErrorCode를 확인한다.
- 새 ErrorCode가 필요하면 담당 영역(PRODUCT / CATEGORY / TRADE / SEARCH)에만 추가 대상으로 제안하고, COMMON 영역은 수정하지 않는다.
- 예외 분석 후에는 리뷰 가능한 작은 작업 단위와 파일 목록을 먼저 제시한다.
- 사용자가 승인한 뒤 테스트를 먼저 작성하고, 실패를 확인한 다음 구현한다.
- 구현 후에는 변경 범위 테스트를 실행하고, 가능하면 전체 테스트도 실행한다.
- 전체 테스트가 실패하면 원인을 분리한다. 실패가 이번 작업 범위가 아니더라도 사용자 승인 전에는 커밋, 푸시, PR 단계로 넘어가지 않는다.

### 브랜치와 PR
- 새 기능은 최신 `develop`에서 새 feature 브랜치를 만들어 작업한다.
- 이미 머지된 로컬 feature 브랜치는 정리하고, 원격 브랜치는 사용자가 요청하지 않으면 삭제하지 않는다.
- 한 브랜치와 한 PR에는 하나의 기능 단위만 담는다.
- 커밋, 푸시, PR 생성은 테스트 결과와 사용자 승인 이후 진행한다.
- Product 담당 작업 중 연결된 코드 수정 때문에 타 담당 패키지나 테스트를 변경해야 하면, 변경 이유와 파일 목록을 답변과 PR 본문에 팀원 공유용으로 명시한다.

### 코드 스타일
- 일반 변수명, 메서드명, DTO 필드명은 camelCase를 사용한다.
- 상수와 enum 값은 UPPER_SNAKE_CASE를 유지한다.
- Lombok을 사용하지 않는다. Kotlin 전환을 어렵게 만들지 않도록 명시적 생성자, getter, 정적 팩토리 메서드를 사용한다.
- Entity에는 불필요한 setter를 만들지 않고, 상태 변경은 의미 있는 도메인 메서드로 표현한다.
- Controller에는 비즈니스 로직을 두지 않는다.
- Entity를 API 응답으로 직접 반환하지 않고 DTO로 변환한다.
- 성공 응답은 `ApiResponse<T>`로 감싼다.
- DTO 변환은 `ProductResponse.from()`처럼 정적 메서드 패턴을 우선 사용한다.
- Spring Data derived query 메서드명이 길면, 메서드 바로 위에 한 줄 주석으로 용도를 설명한다.

### Product Entity 기준
- Product는 `com.dongnemarket.global.common.BaseTimeEntity`를 상속해 `createdAt`, `updatedAt`을 처리한다.
- 논리 삭제 시간은 Product 내부에 `LocalDateTime deletedAt`으로 둔다.
- Member, Category 연관관계는 `@ManyToOne(fetch = FetchType.LAZY)`를 사용한다.
- Product 기본 필드는 `member`, `category`, `title`, `description`, `price`, `tradeStatus`, `region`, `viewCount`, `hidden`, `deletedAt` 기준을 따른다.
- `price` 타입은 `BigDecimal`을 사용한다.
- 상품 생성 기본값은 `tradeStatus = ON_SALE`, `viewCount = 0`, `hidden = false`다.

### 조회와 상태 정책
- 일반 상품 목록과 카테고리별 상품 목록은 삭제되지 않고 숨김 처리되지 않은 상품만 조회한다.
- 목록 정렬은 최신 등록 상품이 먼저 나오도록 id 내림차순을 사용한다.
- 상품 상세 조회는 삭제 상품이면 `DELETED_PRODUCT`, 숨김 상품이면 `HIDDEN_PRODUCT`를 반환한다.
- Favorite, Comment 등 타 도메인에서 상품 접근 가능 여부를 검증할 때는 Product 도메인의 검증 메서드를 사용한다.
- 접근 가능한 상품 검증은 존재하고, `deletedAt`이 null이며, `hidden`이 false인 상품만 통과시킨다.
- 접근 불가능한 상품은 삭제·숨김 여부를 노출하지 않고 `PRODUCT_NOT_FOUND`로 통일할 수 있다.

### 수정, 삭제, 거래 상태 정책
- 상품 수정은 작성자만 가능하다.
- 삭제된 상품은 수정할 수 없다.
- 거래완료(`COMPLETED`) 상품은 내용 수정이 불가능하다.
- 숨김 상품은 작성자라면 수정할 수 있다.
- 상품 삭제는 작성자만 가능하며 논리 삭제로 처리한다.
- 거래완료 상품도 작성자라면 삭제할 수 있다.
- 거래 상태 변경은 작성자만 가능하다.
- 삭제된 상품은 거래 상태를 변경할 수 없다.
- 숨김 상품은 작성자라면 거래 상태를 변경할 수 있다.
- 거래 상태 값은 `ON_SALE`, `RESERVED`, `COMPLETED`만 허용한다.
- 잘못된 거래 상태, blank, null, 필드 누락은 `INVALID_TRADE_STATUS`로 처리한다.
- `COMPLETED` 상품은 `ON_SALE`, `RESERVED`로 되돌릴 수 없고 `CANNOT_CHANGE_COMPLETED_PRODUCT`로 처리한다.
- 같은 상태 요청과 `COMPLETED -> COMPLETED` 요청은 성공 응답으로 현재 상태를 반환한다.

### 테스트와 문서
- 테스트는 Service 테스트와 Controller 테스트를 함께 고려한다.
- Service 테스트는 비즈니스 규칙과 ErrorCode를 검증한다.
- Controller 테스트는 인증, JSON 요청/응답, HTTP status, ErrorCode 매핑을 검증한다.
- Repository 메서드를 추가하면 Repository 테스트로 쿼리 조건을 검증한다.
- API를 추가하거나 변경하면 `docs/postman` 아래에 Postman 시나리오 문서를 추가한다.
- Postman 문서에는 성공, 인증 실패, 권한 실패, 존재하지 않음, 도메인 정책 위반, 입력값 오류 시나리오를 필요한 만큼 포함한다.

### AI 역할 분리
- 작업이 복잡하거나 검증 리스크가 있으면 역할을 나누어 진행한다.
- 분석 AI는 기존 코드, 문서, ErrorCode, 위험 요소를 확인한다.
- 설계 AI는 작은 PR 단위, SOLID 관점, Kotlin 전환 친화성을 검토한다.
- 구현 AI는 승인된 범위 안에서 TDD로 코드를 작성한다.
- 검증 AI는 구현 후 요구사항 누락, 테스트 갭, 문서 누락을 확인한다.

## Product 작업 인수인계 규칙

### 대화와 진행 방식
- 사용자와의 모든 질의응답은 한국어로 한다.
- 사용자는 코드 작성 전 설계, 예외 분석, 파일 목록, 작업 단위 분리를 먼저 확인받는 방식을 선호한다.
- 기능 구현 요청을 받아도 바로 구현하지 말고, 먼저 현재 코드와 문서를 읽고 현황을 요약한다.
- 사용자가 "진행" 또는 명시적으로 승인한 뒤에 테스트 작성과 구현을 시작한다.
- 커밋, 푸시, PR 생성은 사용자의 명시적 승인 이후에만 진행한다.
- PR 생성은 사용자가 직접 확인 후 진행하는 것을 선호하므로, 기본적으로는 PR 본문 초안까지만 작성하고 대기한다.

### 브랜치와 최신화
- 새 작업은 `develop`을 원격 최신 상태로 맞춘 뒤 새 브랜치를 만들어 시작한다.
- 이전 작업 브랜치가 이미 머지되었는지 확인하고, 사용자가 승인하지 않으면 원격 브랜치는 삭제하지 않는다.
- PR 승인 후에는 `develop`으로 돌아와 원격 최신화를 진행하고 워킹트리가 깨끗한지 확인한다.
- 이미 push된 커밋 메시지를 수정해야 할 때는 `git commit --amend` 후 `git push --force-with-lease`를 사용한다.

### 테스트와 검증
- 기능 구현은 테스트 먼저 작성하고 실패를 확인한 뒤 구현한다.
- 단위 테스트와 통합 테스트는 병렬 실행하지 않는다. Gradle `build` 디렉토리 경합을 피하기 위해 반드시 순차 실행한다.
- Product 관련 주요 변경 후에는 변경 범위 테스트를 먼저 실행하고, 필요하면 전체 `./gradlew test`를 실행한다.
- 전체 테스트가 실패하면 구현을 고치지 말고, 실패 클래스와 원인을 먼저 보고하고 사용자의 판단을 기다린다.
- 통합 테스트는 필요한 대상만 좁게 실행한다. 전체 `integrationTest`는 다른 도메인 알려진 실패가 있으면 임의로 돌리지 않는다.

### 타 도메인과 공유
- Product 작업 중 다른 담당자의 패키지나 테스트를 수정해야 하면, 수정 전에 이유와 파일 목록을 보고한다.
- 다른 담당자 코드 변경이 실제로 필요하면 답변과 PR 본문에 팀원 공유 사항으로 따로 명시한다.
- `global`, `member`, `favorite`, `comment`, `report`, `admin` 영역은 임의 수정하지 않는다. 필요한 변경은 요청사항으로 정리한다.
- `BaseInitDataInitializer`처럼 global 영역이지만 Product 기능 정합성에 영향을 받는 파일은 바로 수정하지 말고 팀 공유 후 진행한다.

### Product 이미지 기능 결정사항
- 상품 이미지는 URL 문자열로 저장한다. 파일 업로드, multipart, 스토리지 SDK 연동은 별도 후속 기능이다.
- 상품 등록과 수정 요청은 `imageUrls` 1장 이상 5장 이하를 요구한다.
- 대표이미지는 `thumbnailIndex`로 지정하며, `Product.thumbnailUrl`에 비정규화해 목록 조회에서 사용한다.
- 상세 응답은 `thumbnailUrl`과 `imageUrls` 전체 목록을 함께 반환한다.
- 목록, 검색, 내 상품 응답은 `thumbnailUrl`만 반환한다.
- 이미지 수정은 전체 교체 방식이다. 기존 이미지 row를 productId 기준으로 삭제한 뒤 새 목록을 저장한다.
- `Product`에는 `@OneToMany` 컬렉션을 만들지 않고, `ProductImage -> Product` 단방향 `@ManyToOne(fetch = LAZY)`만 사용한다.
- 기존 이미지 없는 상품은 호환을 위해 `thumbnailUrl = null`, `imageUrls = []`로 응답한다.
- 정식 이미지 저장소가 정해지기 전에는 init 데이터에 개인 저장소나 로컬 이미지 URL을 넣지 않는다.
- init 데이터의 이미지 추가는 결함 수정이 아니라 스토리지 결정 이후 후속 기능으로 본다.

### Product favoriteCount 기능 결정사항
- Favorite와 Product의 순환참조는 Spring Event 기반으로 끊는다.
- Product 도메인은 favoriteCount 수신 측만 담당한다.
- 리스너는 `@EventListener` 동기 same-transaction 방식으로 동작한다.
- 리스너는 `ProductRepository`만 주입받고 FavoriteService, FavoriteRepository, Favorite 엔티티를 참조하지 않는다.
- favoriteCount 증감은 엔티티 로드 후 setter가 아니라 Repository 원자 UPDATE로 처리한다.
- 감소는 `favoriteCount > 0` 조건으로 음수 방지한다.

### 현재 Product API 기준
- `POST /api/products`: 상품 등록, 이미지 1~5장 필수, 대표 이미지 지정.
- `GET /api/products`: 삭제·숨김 제외 목록, 최신순, `thumbnailUrl` 포함.
- `GET /api/products/search`: Specification 기반 검색, 삭제·숨김 제외, 최신순, `thumbnailUrl` 포함.
- `GET /api/products/{productId}`: 상세 조회, 조회수 증가, `thumbnailUrl`과 `imageUrls` 포함.
- `PATCH /api/products/{productId}`: 작성자 수정, 거래완료 수정 금지, 이미지 전체 교체.
- `DELETE /api/products/{productId}`: 작성자 논리 삭제, 거래완료 상품도 삭제 가능.
- `PATCH /api/products/{productId}/status`: 작성자 거래 상태 변경, COMPLETED 되돌리기 금지.
- `GET /api/products/me`: 내 상품 조회, 삭제 제외, 숨김 포함, 최신순.
- `GET /api/categories`: 카테고리 목록 조회.
- `GET /api/categories/{categoryId}/products`: 카테고리별 상품 목록 조회.

### 다음 기능 후보: 위치 기반 상품 조회
- 당근마켓의 "내 동네 2곳"과 유사한 기능은 Product 단독 조회 조건만으로 끝나지 않는다.
- 현재 `Product.region`은 문자열 1개이고, `Member`에는 위치/동네 정보가 없다.
- "내 동네 2곳"을 제대로 구현하려면 회원이 선택한 동네 목록 저장 구조가 필요하다.
- 권장 설계는 `member_locations` 같은 별도 테이블로 회원 1명당 최대 2개 동네를 저장하는 방식이다.
- 다만 이 방식은 Member 영역과 연결되므로 member 담당자 및 팀장과 협의가 필요하다.
- Product 단독으로 작게 시작하려면 요청 파라미터로 지역 목록을 받아 상품을 필터링하는 방식도 가능하지만, 이는 진짜 "내 동네" 기능은 아니다.
