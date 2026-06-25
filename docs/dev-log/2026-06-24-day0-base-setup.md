# 🏪 동네마켓 API — Day 0 공통 베이스 세팅 완료 공유

**작성:** 조민석(팀장) · **날짜:** 2026-06-24 · **기준 브랜치:** `develop` (고정 완료)

> ✅ 오늘 공통 베이스(global)를 `develop`에 올려 **고정 완료**했습니다.
> 이제 각자 도메인 개발을 시작하면 됩니다. 모두 **같은 응답·예외·보안 구조** 위에서 출발합니다.

---

## 1. 기술 스택

| 항목 | 내용 |
| --- | --- |
| 언어 / 런타임 | Java 21 (Gradle toolchain 자동 고정) |
| 프레임워크 | Spring Boot 3.5.15 |
| 빌드 | Gradle 8.14.5 (Wrapper 포함) |
| DB | MySQL 8 (앱) / H2 (테스트 자동) |
| 보안 | Spring Security + JWT (jjwt 0.12.6) |
| API 문서 | Swagger (springdoc 2.8.6) |
| 기타 | Spring Data JPA, JUnit5 · **Lombok 미사용**(Kotlin 마이그레이션 대비) |
| 베이스 패키지 | `com.dongnemarket` |

---

## 2. 시작하기 (팀원 필독)

```bash
git clone https://github.com/prgrms-be-devcourse/NBE9-11-final-Team7.git
cd NBE9-11-final-Team7
git checkout develop
git pull origin develop

cd backend          # ★ 모노레포: 백엔드 프로젝트는 backend/ 아래 (이후 명령은 여기서)

# (선택) 로컬 MySQL 한 번에 띄우기
docker compose up -d

# 빌드 + 테스트 (JDK 21 자동 준비, 테스트는 H2라 MySQL 없어도 통과)
./gradlew build

# 실행 → http://localhost:8080/swagger-ui.html
./gradlew bootRun
```

> 💡 첫 빌드는 JDK 21 toolchain·의존성을 받느라 시간이 걸립니다. 이후엔 빨라요.
> 💡 DB 기본값: DB `dongne_market` / 계정 `dongne` / 비번 `dongne1234` (개발용). 운영 시 환경변수 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`·`JWT_SECRET`로 주입.
> ⚠️ public 저장소입니다 — 시크릿은 절대 커밋 금지 (`application-secret.yml`·`.env`는 `.gitignore`로 차단됨).
> 💡 **모노레포 구조**: IntelliJ는 레포 루트가 아니라 **`backend/build.gradle`** 을 Gradle 프로젝트로 열어 import. 빌드·실행 명령은 모두 `backend/`에서.

---

## 3. 프로젝트 구조 (global = 팀장 관리 영역)

```
com.dongnemarket
├── global
│   ├── common      BaseTimeEntity (createdAt/updatedAt 자동)
│   ├── config      JpaAuditingConfig, SwaggerConfig
│   ├── response    ApiResponse<T>, ErrorResponse
│   ├── exception   ErrorCode, BusinessException, GlobalExceptionHandler
│   └── security    SecurityConfig + jwt/(Provider·Filter·EntryPoint·AccessDeniedHandler)
└── (auth · member · product · category · favorite · comment · report · admin · trade · search)
        ↑ 각 팀원이 자기 도메인 패키지를 여기에 생성
```

---

## 4. ✅ 꼭 지킬 공통 규칙

> 이것만 지키면 충돌 없이 합쳐집니다.

- **성공 응답**은 `ApiResponse.success(data)` 로 감싸기
- **예외**는 `throw new BusinessException(ErrorCode.XXX)` — RuntimeException 직접 던지기 ❌
- **ErrorCode**는 `ErrorCode.java`의 **자기 도메인 주석 영역에만** 추가 (COMMON·다른 도메인 영역 ❌)
- **Entity 직접 반환 ❌** → Request / Response DTO 분리
- 계층 책임: Controller(검증·호출·응답) / Service(비즈니스 로직·트랜잭션) / Repository(DB 접근만)
- **담당 도메인 패키지 밖 수정 ❌**, `global`·`SecurityConfig`·COMMON ErrorCode는 팀장만
- **Lombok 사용 ❌** — 프로젝트에서 제거됨(Kotlin 마이그레이션 대비). 게터·생성자·logger는 직접 작성 (`@Getter`·`@Builder`·`@NoArgsConstructor`·`@Slf4j` 금지)

### 코드 예시

```java
// 1) 성공 응답
return ResponseEntity.ok(ApiResponse.success(responseDto));

// 2) 예외 던지기
if (product == null) {
    throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
}

// 3) ErrorCode 추가 — ErrorCode.java 내 '자기 도메인' 주석 영역에만
//   // ===== FAVORITE ERROR (권건우) =====
//   FAVORITE_ALREADY_EXISTS(409, "FAVORITE_001", "이미 관심 등록한 상품입니다."),

// 4) 로그인 성공 시 토큰 발급 (auth 도메인)
String accessToken = jwtTokenProvider.createAccessToken(member.getId(), "ROLE_USER");
```

### 공통 응답 포맷

```json
// 성공
{ "status": 200, "message": "요청이 성공적으로 처리되었습니다.", "data": { } }

// 에러
{ "status": 404, "error": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다.", "timestamp": "2026-06-24T15:00:00" }
```

---

## 5. 인증 / 보안 (이미 적용되어 있음)

- 헤더 `Authorization: Bearer {accessToken}`, 무상태(stateless) JWT
- **인증 불필요**: 회원가입·로그인, 상품 목록/상세, 카테고리 조회, 댓글 목록 조회
- `/api/admin/**` → `ROLE_ADMIN` 전용
- 미인증(401)·권한없음(403)은 공통 `ErrorResponse`(JSON)로 자동 응답
- Swagger UI 우측 상단 **Authorize** 에 토큰 넣고 보호 API 테스트 가능

---

## 6. 브랜치 · PR 규칙 (Git Flow Lite)

```
main       발표·안정 버전 (팀장만 merge)
 └ develop   통합 브랜치 (항상 green) ← 여기로 PR
     └ feature/{도메인}_{기능}   각자 작업 브랜치
```

- `develop`에서 분기 → `feature/도메인_기능` → 작은 PR → `develop`
- **PR은 작게, 자주.** 한 PR = 한 작업 단위 (여러 기능 섞지 않기)
- 커밋 메시지: `feat: 회원가입 API 구현` (타입: feat / fix / docs / refactor / test / chore)
- PR 올리면 CI(빌드) 자동 실행 + 팀장 리뷰 후 머지

---

## 7. 오늘 진행한 과정

- [x] Spring Initializr로 프로젝트 스캐폴딩 (Java 21 / Boot 3.5.15)
- [x] `build.gradle` (springdoc·jjwt 추가) · Wrapper · `application.yml`/`-test.yml` · `docker-compose.yml`
- [x] 공통 응답·예외: ApiResponse, ErrorResponse, ErrorCode(도메인별 골격), BusinessException, GlobalExceptionHandler
- [x] BaseTimeEntity + JPA Auditing
- [x] Security(JWT) + Swagger 설정 (URL 권한 정책 포함)
- [x] 테스트: `contextLoads` + 보안 정책(401·Swagger 공개) → `./gradlew build` ✅ **green**
- [x] Git Flow Lite: `main`·`develop` 생성·push, GitHub 협업 설정(브랜치 보호 등)

---

## 8. 다음 단계 (각자)

1. `docs/ai/00-ai-common-rules.md` 읽기 → 역할 선택 → 자기 역할 문서(01~04)로 이동
2. 5단계 흐름으로 **첫 기능**부터: ① ErrorCode → ② 구현 → ③ 단위 테스트 → ④ Postman → ⑤ PR
3. 기능이 완성되는 대로 작은 PR을 `develop`로 (하루치 몰아두지 않기)

> 질문은 팀 채널 / 팀장에게 🙌
