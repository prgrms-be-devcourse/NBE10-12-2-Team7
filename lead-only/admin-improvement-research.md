# Admin 도메인 개선·확장·기술도입 조사 보고서

> 30년차 백엔드 관점에서 현재 admin 코드를 보고 **① 코드 개선 ② 기능 추가 ③ 새 기술 도입**을 조사한 문서.
> 작성일 2026-06-29. 읽고 직접 취사선택하기 위한 자료이며, 당장 다 할 필요는 없다.
>
> **범례** — 가치: 🔴필수급 · 🟡권장 · 🟢선택 / 난이도: S(반나절) · M(1~3일) · L(1주+) / 판정: **지금** · 나중 · 학습용 · 스킵

---

## 0. 현재 상태 스냅샷

**스택**: Spring Boot 3.5.15 · Java 21 · Spring Data JPA(Hibernate 6.6) · Spring Security · MySQL(운영/개발)·H2(테스트만) · springdoc-openapi 2.8.6 · jjwt 0.12.6. validation 스타터 **포함**.

**어드민 커버리지(13 엔드포인트)**: Member(목록·상세·상태변경) · Product(목록·상세·숨김·삭제) · Comment(목록·삭제) · Report(목록·상세·상태변경) · Dashboard. 계정 시드 1개.

**구조 제약(조사에 직접 영향)**:
| 제약 | 의미 |
|---|---|
| `open-in-view: false` | 컨트롤러/뷰에서 LAZY 로딩 불가 → 매핑을 트랜잭션(서비스) 안에서 끝내야 함. 지금 `DTO.from()`이 서비스 안에서 돌아 **동작은 하지만 N+1** |
| `ddl-auto: update`, 마이그레이션 도구 없음 | 새 기능마다 스키마가 손으로 바뀜 → 운영 반영·롤백 위험. Flyway/Liquibase 공백 |
| validation 의존성 있으나 admin 미사용 | `@Valid` + enum 바인딩으로 즉시 개선 가능 |
| access 토큰만, refresh 없음, TTL 3600s | 정지/삭제가 **최대 1시간** 효력 없음(N2). refresh/회전 메커니즘 부재 |
| ErrorCode COMMON은 팀장(본인)만 수정 | 공통 에러(ProblemDetail 전환 등) 변경 권한이 본인에게 있음 |

---

## Part 1 — 코드 개선 (기존 admin 코드 리팩터/보강)

### 성능·확장성
| # | 문제(위치) | 30년차 진단 → 처방 | 기술 | 난이도 | 가치 |
|---|---|---|---|---|---|
| C1 | 목록 전건 `findAll()` (Admin*Service 4곳) | 관리자 목록은 페이징이 전제. 데이터 수천건만 넘어도 체감 → `Pageable`+`Page<T>`, 기본 정렬 `createdAt desc` | Spring Data Pageable | S | 🔴 |
| C2 | N+1 (`AdminProductResponse.from` → `getMember()/getCategory()`) | 상품 100개=쿼리 101회. id만 필요하니 **DTO 프로젝션**이 깔끔(또는 `@EntityGraph`) | `@EntityGraph` / JPQL projection / QueryDSL | M | 🔴 |
| C3 | 대시보드 매 호출 `count()`×5 | 실시간 정확도 불필요 → 짧은 TTL 캐시 또는 단일 집계쿼리 | Spring Cache + Caffeine | S | 🟡 |

### 도메인 정합성
| # | 문제 | 처방 | 난이도 | 가치 |
|---|---|---|---|---|
| C4 | 상태 전이 무검증 (`Report/Member.changeStatus`가 `this.status=status`) | 종료상태(COMPLETED/REJECTED/DELETED) 재전이 허용됨. **엔티티 안에 상태머신**(허용 전이만, 위반 시 도메인 예외) | M | 🔴 |
| C5 | soft delete 비일관 (목록은 삭제포함 `findAll`, 댓글삭제는 `...DeletedAtIsNull`) | 규칙 명문화 + 선언적 강제 검토(`@SQLRestriction`/`@SoftDelete`). 단 어드민은 삭제건도 봐야 하므로 우회 경로 필요 — 트레이드오프 인지 | M | 🟡 |
| C6 | 동시 수정 lost update (락 없음) | 중요 엔티티에 `@Version` 낙관적 락 → "정지 처리가 조용히 덮어써지는" 사고 방지 | S | 🟡 |
| C7 | 복구 시 `deletedAt` 소실 (`Member.changeStatus`, N4) | DELETED→ACTIVE 시 최초 삭제시각 날아감. C4 상태머신에 흡수 | S | 🟡 |

### 보안·운영
| # | 문제 | 처방 | 난이도 | 가치 |
|---|---|---|---|---|
| C8 | **관리자 상호 잠금 가드 없음 (N1)** | `changeMemberStatus`가 대상 role 무검사 → 다른/시드 관리자 정지·삭제 시 로그인 영구 잠금(시드 멱등이라 복구 안 됨). 대상 `ROLE_ADMIN`(또는 본인) 거부, last-admin 가드 | S | 🔴 |
| C9 | **정지/삭제 토큰 즉시 무효화 불가 (N2)** | 상태검사가 `AuthService.login`뿐, TTL 1h. ①JWT 필터에서 상태 재조회(간단, 요청당 1쿼리) ②Redis 블랙리스트 ③TTL 단축+refresh 회전 | M~L | 🔴 |
| C10 | 시드 비번 하드코딩 `admin1234!` (C2-리뷰) | VCS 유출. env/secret 외부화 + 최초 로그인 변경 강제 또는 운영 미시드 | S | 🔴 |
| C11 | 행위자·감사 부재 (C1-리뷰) | 컨트롤러가 acting admin조차 안 받음(`@AuthenticationPrincipal` 없음). principal 수신부터 → Part 2 감사로그로 연결 | M | 🔴 |
| C12 | 권한 단일 `ROLE_ADMIN` (C3-리뷰) | 읽기전용 감사자/조치권자 구분 불가. authority 기반 + `@PreAuthorize`로 진화 여지 | M | 🟡 |

### 설계·품질
| # | 문제 | 처방 | 난이도 | 가치 |
|---|---|---|---|---|
| C13 | 검증이 DB 조회 **후** (`parseStatus` after `findById`) | 잘못된 입력인데 DB 먼저 침. 검증 선행 or 요청 DTO enum 바인딩+`@Valid`(의존성 이미 있음) | S | 🟡 |
| C14 | 크로스도메인 직접 변경 (admin이 `member.changeStatus()` 직접 호출) | 소유 도메인 규칙·이벤트(예: 정지 알림) 우회. `MemberService.suspendByAdmin(...)` 경유 또는 도메인 이벤트 | M | 🟡 |
| C15 | 무거운 `@SpringBootTest` + H2(운영은 MySQL) | 슬라이스(`@WebMvcTest`/`@DataJpaTest`)로 대부분 전환 + **Testcontainers**로 DB 패리티·격리 | M | 🟡 |
| C16 | `DTO.from()` 수동 매핑 반복 | 보일러플레이트. MapStruct로 줄일 수 있으나 가독성 트레이드오프 | S | 🟢 |

---

## Part 2 — 기능 추가 (신규 역량)

### 모더레이션 실전화 — *가장 큰 제품 공백*
| 기능 | 현황·근거 | 설계 스케치 | 기술 | 난이도 | 가치 |
|---|---|---|---|---|---|
| 신고→대상 연결 조회 | `Report`가 ID만 보유 → 판단 불가 | 신고 상세에 대상 회원/상품 요약 + **해당 대상 누적 신고 수·이력** 조인 | 집계쿼리/프로젝션 | M | 🔴 |
| 신고 처리 ↔ 조치 연계 | 지금은 상태만 바뀌고 제재는 수작업 | COMPLETED 시 대상 상품 숨김/회원 정지를 **한 트랜잭션**에(+감사로그) | 트랜잭션·도메인 이벤트 | M | 🔴 |
| 기간제 정지 + 사유 | SUSPENDED 무기한·무사유 | `Member`에 `suspendedUntil`,`suspendReason` + 만료 자동복구 배치 | 엔티티 확장 + `@Scheduled` | M | 🟡 |
| 회원 360° 뷰 | 맥락이 흩어짐 | 회원 상세 = 그의 상품·댓글·받은 신고 묶음 | 다중도메인 조회 | M | 🟡 |

### 관측·감사
| 기능 | 설계 스케치 | 기술 | 난이도 | 가치 |
|---|---|---|---|---|
| 관리자 활동 로그(audit) | `AdminAuditLog`(행위자·시각·대상·전/후값) 별도 테이블, 횡단 기록 | Spring AOP 또는 도메인 이벤트 | M | 🔴 |
| 대시보드 심화 | 기간별 추이(가입·신고·거래), Top 신고 대상, **신고 처리율(SLA)** | group by/시계열 집계 DTO | M | 🟡 |
| 목록 검색·필터 | 상태/유형/사유/기간 동적 필터(+C1 페이징) | Specification 또는 QueryDSL | M | 🔴 |

### 마스터데이터·커뮤니케이션
| 기능 | 근거 | 기술 | 난이도 | 가치 |
|---|---|---|---|---|
| 카테고리 관리(CRUD) | `Category` 엔티티 존재하나 어드민 관리 없음 | 기본 CRUD(어드민 컨벤션 재현) | S | 🟡 |
| 제재 알림 | 알림 시스템 자체가 없음 → 정지/삭제 통지 불가 | **새 도메인** `Notification` + 도메인 이벤트 발송 | L | 🟡 |

---

## Part 3 — 새 기술 도입 후보 (이 프로젝트 기준 평가)

| 기술 | 여기서 푸는 문제 | 적용 위치 | 도입비용·리스크 | 학습가치 | 판정 |
|---|---|---|---|---|---|
| **Spring Data Pageable** | 목록 페이징(C1) | Admin*Service/Controller | 매우 낮음(기본 포함) | 중 | **지금** |
| **`@EntityGraph` / JPQL 프로젝션** | N+1(C2) | Admin*Repository | 낮음 | 높음 | **지금** |
| **Bean Validation `@Valid`** | 검증 위치(C13) — 의존성 이미 있음 | 요청 DTO | 매우 낮음 | 중 | **지금** |
| **`@Version` 낙관적 락** | 동시수정(C6) | Member/Report/Product | 낮음(필드+예외처리) | 높음 | 지금 |
| **Spring Cache + Caffeine** | 대시보드 count(C3) | DashboardService | 낮음(스타터+`@Cacheable`) | 중 | 나중 |
| **Specification / QueryDSL** | 동적 필터·검색(Part2) | 신규 검색 메서드 | Spec=낮음 / QueryDSL=중(Jakarta 빌드설정) | 높음 | 나중(필터 착수 시) |
| **Spring AOP / 도메인 이벤트** | 감사로그·조치연계(C11,Part2) | `@TransactionalEventListener` | 중(설계 이해 필요) | 매우 높음 | 나중 |
| **Spring Security 메서드 시큐리티** | 권한 세분화(C12) | `@PreAuthorize`+authority | 중 | 높음 | 나중 |
| **Hibernate `@SoftDelete`/`@SQLRestriction`** | soft delete 일관(C5) | 엔티티 | 중(어드민 삭제건 조회와 충돌 주의) | 높음 | 학습용/나중 |
| **Spring Data JPA Auditing + Envers** | 변경 이력·`@CreatedBy/@LastModifiedBy`(C11) | BaseEntity + `AuditorAware` / Envers | Auditing=낮음 / Envers=중 | 높음 | 나중 |
| **Flyway / Liquibase** | `ddl-auto=update` 위험·스키마 진화 | resources/db/migration | 중(기존 스키마 baseline) | 매우 높음(실무 필수) | 나중(스키마 변경 잦아지면 **지금급**) |
| **Testcontainers** | H2↔MySQL 불일치·격리(C15) | 테스트 | 중(Docker 필요) | 매우 높음 | 나중 |
| **Refresh 토큰 + 회전** | 토큰 무효화·세션수명(C9) | auth | 중~높음 | 매우 높음 | 나중 |
| **Redis** | 토큰 블랙리스트·캐시·rate limit(C9) | 인프라 | 높음(운영 컴포넌트 추가) | 높음 | 나중/스킵(범위↑) |
| **ProblemDetail(RFC 7807)** | 에러응답 표준화 | GlobalExceptionHandler | 낮음(Spring 6 내장) | 중 | 학습용 |
| **MapStruct** | DTO 매핑 보일러(C16) | dto | 낮음 | 낮음 | 선택 |
| **Micrometer + Actuator** | 운영 지표·헬스 | 전역 | 중 | 중 | 나중 |
| **Java 21 가상 스레드** | I/O 동시성 | `spring.threads.virtual.enabled` | 낮음 | 중 | 스킵(어드민엔 과함) |

---

## Part 4 — 추천 로드맵 (단계별, 결정용)

**Phase 1 · 토대 굳히기 (저비용·고효과, S 위주)**
→ C1 페이징 · C2 N+1 · C13 `@Valid` · C8 관리자 잠금 가드(N1) · C10 시드 비번 외부화.
*효과*: "데이터 늘면 터지는 곳"과 "보안 사고"를 먼저 막음. 거의 다 반나절급.

**Phase 2 · 도메인 실전화 (제품 가치, M 위주)**
→ C4 상태머신 · C11 행위자 수신 + 감사로그(AOP/이벤트) · 신고→대상 연결 · 신고 처리↔조치 연계 · 목록 필터(Specification).
*효과*: 어드민이 "실제로 쓸 수 있는" 모더레이션 도구가 됨. 도메인 설계·AOP·이벤트를 함께 학습.

**Phase 3 · 인프라·확장 (L, 운영 성숙도)**
→ Flyway 도입 · Testcontainers 전환 · refresh 토큰/Redis로 N2 정공법 · 알림 도메인 · 권한 세분화(`@PreAuthorize`).
*효과*: 학습 프로젝트 → 운영 가능 수준. 실무 핵심 기술 경험.

---

## 부록 — 빠른 결정 매트릭스 (Top 픽)

| 우선 | 항목 | 유형 | 가치 | 난이도 |
|---|---|---|---|---|
| 1 | C8 관리자 잠금 가드(N1) | 개선 | 🔴 | S |
| 2 | C10 시드 비번 외부화 | 개선 | 🔴 | S |
| 3 | C1 페이징 + C2 N+1 | 개선 | 🔴 | S~M |
| 4 | C4 엔티티 상태머신 | 개선 | 🔴 | M |
| 5 | 감사로그 + 행위자(C11) | 기능 | 🔴 | M |
| 6 | 신고→대상 연결 + 조치 연계 | 기능 | 🔴 | M |
| 7 | Flyway 마이그레이션 | 기술 | 🟡 | M |
| 8 | Testcontainers | 기술 | 🟡 | M |

> 한 줄 결론: **Phase 1(가드·비번·페이징·N+1·@Valid)은 거의 공짜에 가깝게 효과가 크니 먼저**, 그다음 무엇을 "기능"으로 키울지(모더레이션 vs 감사 vs 알림)를 골라 Phase 2로. 새 기술은 그 기능에 **딸려서** 자연스럽게 도입하는 게 학습·정착 모두 유리하다.
