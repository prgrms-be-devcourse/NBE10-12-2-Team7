# AGENTS.md — AI 에이전트 진입 문서

> **어떤 AI 에이전트든(Claude Code · Cursor · Gemini 등) 이 저장소에서 작업을 시작하기 전에 이 파일을 먼저 읽는다.**
> 이 문서는 프로젝트를 이해하고 개발 프로세스를 지키기 위한 단일 진입점이다. 세부는 [`docs/`](docs/README.md)로 연결한다.
> 최종 수정일: 2026-07-07

---

## 1. 프로젝트 한눈에

- **마켓온** — 지역 기반 중고거래 서비스. **모노레포**(백엔드 + 프론트 + 인프라).
- 백엔드: **Spring Boot 3.5 / Java 21** · Spring Security(JWT) · JPA · MySQL 8 · Springdoc(Swagger)
- 프론트: **Next.js 16**(App Router) · React 19 · TypeScript · Tailwind
- 인프라: Docker Compose · nginx · Prometheus·Loki·Grafana · Cloudflare Tunnel
- AI: Spring AI + 사내 Ollama (관리자 AI 어시스턴트, `admin.ai`)

## 2. 코드 지도 (어디를 고치나)

백엔드 패키지 루트: `com.dongnemarket` (`backend/src/main/java/com/dongnemarket/`).
**도메인별 패키지 + 계층형** 구조. 작업은 **자기 도메인 패키지 안에서만** 한다.

| 도메인 | 패키지 | 책임 |
| --- | --- | --- |
| `auth` | `auth/` | 회원가입·로그인·JWT·이메일인증·비번재설정 |
| `member` | `member/` | 내 정보·동네(위치) |
| `product` | `product/` | 상품·이미지·검색 |
| `category` | `category/` | 카테고리 |
| `favorite` | `favorite/` | 관심 |
| `comment` | `comment/` | 댓글 |
| `report` | `report/` | 신고 |
| `notification` | `notification/` | 알림(댓글·가격변경) |
| `chat` | `chat/` | 1:1 채팅 |
| `region` | `region/` | 지역 사전 |
| `admin` | `admin/` (+`admin/ai/`) | 운영 관리 + AI 어시스턴트 |
| `global` | `global/` | **공통(팀장 소유)** — 응답·예외·보안·설정 |

**계층 규칙**: `Controller → Service → Repository → Entity/DTO`
- Controller: 요청 수신·`@Valid`·`ApiResponse` 반환·Swagger. 비즈니스 로직 금지.
- Service: 비즈니스 로직·검증·트랜잭션. 예외는 `BusinessException(ErrorCode)`.
- Repository: DB 접근만. Entity는 응답으로 직접 반환 금지(DTO 분리).

## 3. 정본 (grounding — 지어내지 말고 여기서 확인)

| 알고 싶은 것 | 정본 |
| --- | --- |
| API 스펙(요청/응답 스키마) | **Swagger** `http://localhost:8080/swagger-ui.html` (코드에서 자동생성) |
| 성공/에러 응답 형식 | `global/response/ApiResponse`, `global/exception` |
| 시스템 구조·ERD | [`docs/architecture/`](docs/architecture/README.md) |
| 실행·환경 세팅 | [`docs/getting-started/`](docs/getting-started/README.md) |
| 기술 결정의 이유 | [`docs/adr/`](docs/adr/README.md) |
| 배포·운영 | [`docs/runbook/`](docs/runbook/README.md) |

**주요 명령** (리포 루트에서):
```bash
# dev (빠른 루프)
docker compose up -d --wait          # MySQL만
cd backend && ./gradlew bootRun      # 백엔드 :8080
cd frontend && npm install && npm run dev   # 프론트 :3000
# 테스트 / 빌드
cd backend && ./gradlew test         # 단위·통합 테스트
cd backend && ./gradlew clean build -x test   # 앱 이미지용 JAR
```

## 4. 개발 흐름 (모든 기능 공통)

```
① 착수 전   → Notion WBS 'task 03.개발'에 기능 단위 "개발할 것" 문서 작성
② 브랜치    → develop에서 feature/{도메인}_{기능} 분기
③ 개발      → ErrorCode → 구현 → 단위 테스트 → 통합 테스트 → API 테스트(Postman) → PR
④ PR        → feature/* → develop, 작게·자주. PR 템플릿 채움
⑤ 마무리    → 문서 영향 확인 후 docs 갱신(아래 5번), develop은 항상 green 유지
```
상세 규칙: [`docs/conventions/git-collaboration.md`](docs/conventions/git-collaboration.md).
커밋 타입: `feat` · `fix` · `docs` · `refactor` · `test` · `chore`.

## 5. ★ 문서 동기화 규칙 (에이전트가 반드시 집행)

> 이 프로젝트의 최우선 과제: **코드가 앞서가고 문서가 뒤처지는 드리프트를 막는 것.**
> 코드를 변경한 **바로 그 작업(같은 PR)에서** 아래 매핑에 따라 문서를 함께 갱신한다. "나중에"는 없다.

| 변경 종류 | 해야 할 문서 작업 |
| --- | --- |
| 새 **도메인/컨테이너** 추가, 구조 변경 | [`docs/architecture/`](docs/architecture/README.md) 갱신 (C4·ERD) |
| 새 **기술/라이브러리/구조 패턴** 도입 | **[`docs/adr/`](docs/adr/README.md)에 새 ADR 작성** (기존은 수정 금지, 번호만 추가) |
| 새 **엔드포인트** 추가/변경 | Swagger는 코드에서 자동 → [`docs/api/`](docs/api/README.md) 리소스 지도만 반영 |
| **프로세스/규칙** 변경 | [`docs/conventions/git-collaboration.md`](docs/conventions/git-collaboration.md) |
| 위에 없는 일반 코드 변경 | 문서 변경 불필요 |

**두 가지 하드 규칙:**
1. **새 기술 도입은 ADR 없이 완료 처리하지 않는다.** (라이브러리 추가·구조 결정·패턴 변경 포함)
2. **문서 변경은 코드 변경과 같은 PR에 담는다.** 별도 "문서 정리" PR로 미루지 않는다.

## 6. 가드레일 (하지 말 것)

```
- 담당 도메인 외 패키지 수정
- global 공통 구조·SecurityConfig·공통 응답/에러 구조 변경 (팀장 영역)
- COMMON ErrorCode 영역 수정 / Entity 연관관계 임의 변경 / API URI 임의 변경
- Entity를 API 응답으로 직접 반환 (Request/Response DTO 분리)
- 테스트·API 검증 없이 기능 완료 처리
- 위 5번 문서 동기화를 건너뛰고 코드만 머지
```

## 7. 더 읽기

전체 문서 지도는 [`docs/README.md`](docs/README.md). 읽는 순서 추천: getting-started → architecture → conventions → (필요시) adr·api·runbook.
