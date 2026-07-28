# backend — Spring Boot API 서버

마켓온의 API 서버. **Spring Boot 3.5 / Java 21**, 도메인별 패키지 + 계층형 구조.

> 이 문서는 `backend/`를 이해하는 진입점이다. 리포 전체는 [../README.md](../README.md), 에이전트 작업 규칙은 [../AGENTS.md](../AGENTS.md).

## 스택

| | |
|---|---|
| 프레임워크 | Spring Boot 3.5, Java 21 |
| 보안 | Spring Security + JWT |
| 영속성 | JPA(Hibernate), MySQL 8 / 테스트는 H2 |
| 스키마 | **Flyway** 마이그레이션 (`src/main/resources/db/migration/`). 운영은 `ddl-auto: validate` |
| 문서 | Springdoc(Swagger) — 코드에서 자동 생성 |
| AI | Spring AI + Ollama (관리자 AI 어시스턴트 `admin/ai`) |

## 도메인 지도

패키지 루트는 `com.dongnemarket` (`src/main/java/com/dongnemarket/`).
**작업은 자기 도메인 패키지 안에서만** 한다.

| 도메인 | API 베이스 | 책임 |
|---|---|---|
| `auth` | `/api/auth`, `/api/auth/email-verifications`, `/api/auth/password-resets` | 회원가입·로그인·JWT·이메일 인증·비밀번호 재설정 |
| `member` | `/api/members`, `/api/members/me/locations` | 내 정보·동네(지역) 설정 |
| `product` | `/api/products`, `/api/products/me` | 상품·이미지·검색·노출 우선순위 |
| `category` | `/api/categories` | 카테고리 |
| `region` | `/api/regions` | **계층형 지역 마스터**(시-구-동), `regionCode` 기준 |
| `favorite` | — | 찜. 이벤트로 `Product.favoriteCount` 증감 |
| `comment` | — | 댓글 |
| `report` | — | 신고 |
| `notification` | — | 알림(댓글·가격변경·채팅). **저장형이 아니라 조회 시점 파생** |
| `chat` | — | 1:1 채팅 |
| `trade` | — | 거래 내역 |
| `escrow` | `/api/escrows` | **안심결제(에스크로)** — 단일 Trade 애그리거트, 실송금은 모사 |
| `manner` | — | 매너온도 |
| `admin` | `/api/admin/*` (ai · members · products · comments · reports · dashboard · manner-scores · storage) | 운영 관리 + AI 어시스턴트 |
| `global` | — | **공통(팀장 소유)** — 응답·예외·보안·설정·시더 |

## 계층 규칙

```
Controller → Service → Repository → Entity/DTO
```

- **Controller** — 요청 수신, `@Valid`, `ApiResponse` 반환, Swagger 어노테이션. **비즈니스 로직 금지.**
- **Service** — 비즈니스 로직·검증·트랜잭션. 예외는 `BusinessException(ErrorCode)`.
- **Repository** — DB 접근만.
- **Entity를 API 응답으로 직접 반환 금지** — Request/Response DTO를 분리한다.

## 정본 (지어내지 말고 여기서 확인)

| 알고 싶은 것 | 어디를 보나 |
|---|---|
| API 요청/응답 스키마 | **Swagger** `http://localhost:8080/swagger-ui.html` (코드에서 자동 생성) |
| 성공/에러 응답 형식 | `global/response/ApiResponse`, `global/exception/` |
| 에러 코드 목록 | `global/exception/ErrorCode` |
| 스키마·테이블 구조 | `src/main/resources/db/migration/V*.sql` |
| 시드 데이터 | `global/init/`, `src/main/resources/seed/` |

## 실행

```bash
docker compose up -d --wait     # 리포 루트에서 — MySQL만 기동
./gradlew bootRun               # :8080
```

## 테스트

```bash
./gradlew test                          # 단위 + 슬라이스 (H2)
./gradlew test --tests "com.dongnemarket.favorite.*"   # 도메인별
./gradlew clean build -x test           # 앱 이미지용 JAR
```

- 단위 테스트는 목킹 기반, 통합 테스트는 `@SpringBootTest`·`@DataJpaTest`.
- 통합 테스트는 "코드만 읽어도 흐름이 보이는" 실행 명세서로 쓴다 — GWT 구조, 시나리오 네이밍, 실제 사용자 여정.

## 주의

- **CORS 설정 없음** — 브라우저는 단일 origin만 호출하고 `/api`는 서버가 프록시한다.
- `global/` 공통 구조(SecurityConfig·공통 응답/에러)는 팀장 영역이다.
- 새 도메인을 추가하면 이 문서의 도메인 지도를 **같은 PR에서** 갱신한다.
