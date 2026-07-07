# 마켓온 · 기술 문서 (docs)

이 폴더는 **코드와 함께 관리되는 기술 문서**만 둔다. 시스템을 이해하고(구조·데이터·결정), 운영하고(배포·장애 대응), 규칙을 맞추는(컨벤션) 데 필요한 문서다.

> **원칙 — Docs-as-Code**
> "이 문서가 코드와 함께 바뀌어야 하나?" → **Yes**면 여기(`docs/`), **No**(회의록·기획·일정 등 프로세스)면 Notion/위키에 둔다.
> 관련 코드 PR에 문서 변경을 **함께 포함**해서, 문서가 코드와 어긋나지 않게 유지한다.

---

## 이 프로젝트가 뭔가 (한눈에)

- **마켓온** — 지역 기반 중고거래 서비스. **모노레포**(Spring Boot 백엔드 + Next.js 프론트 + 인프라).
- 백엔드: Spring Boot 3.5 · Java 21 · Spring Security(JWT) · JPA · MySQL 8
- 프론트: Next.js 16(App Router) · React 19 · TypeScript · Tailwind
- 인프라: Docker Compose · nginx · Prometheus·Loki·Grafana · Cloudflare Tunnel
- AI: Spring AI + 사내 Ollama (관리자 AI 어시스턴트)

> **로컬 실행/띄우는 법은 루트 [README.md](../README.md) 참고** (dev · local-deploy).

---

## 문서 인덱스

목표 구조와 현재 상태다. 비어 있는 항목은 develop 기준으로 **단계적으로 채워 간다.**

| 폴더 | 내용 | 상태 |
| --- | --- | --- |
| [`getting-started/`](getting-started/) | [로컬 실행·개발 환경 세팅](getting-started/README.md) (신규 투입자가 제일 먼저) | ✅ 작성됨 |
| [`architecture/`](architecture/) | 시스템 구조 — [컨텍스트](architecture/01-context.md)·[컨테이너](architecture/02-container.md)·[컴포넌트](architecture/03-component.md)·[ERD](architecture/04-erd.md) | ✅ 작성됨 |
| [`adr/`](adr/) | ★ 기술 결정 기록 — [0001 Docs-as-Code 재편](adr/0001-adopt-docs-as-code-structure.md) | ✅ 시작됨 |
| [`api/`](api/) | [API 명세](api/README.md) — Swagger 정본 + 공통 규약·리소스 지도 | ✅ 작성됨 |
| [`runbook/`](runbook/) | [운영·장애 대응·배포 절차](runbook/README.md) | ✅ 작성됨 |
| [`conventions/`](conventions/) | 코딩·커밋·브랜치 규칙 — [Git 협업 규칙](conventions/git-collaboration.md) | ✅ 시작됨 |

> 채워지는 대로 상태를 ✅ 로 바꾸고 대표 문서 링크를 건다.

---

## 무엇을 어디에 두나 (경계)

문서를 어디에 남길지 헷갈릴 때의 기준이다.

| 대상 | 위치 | 이유 |
| --- | --- | --- |
| 아키텍처 · ERD · 기술 결정(ADR) | **`docs/`** | 코드와 같이 버전 관리·리뷰돼야 함 |
| API 명세(정본) | **코드 → Swagger/Springdoc** | 컨트롤러에서 자동 생성 = 항상 최신 |
| API 테스트 컬렉션(Postman) | **개인 / Postman 워크스페이스** | Swagger에서 import해 쓰는 파생물 |
| 비밀값(토큰·비번·로컬 설정) | **개인 · `.env`** (커밋 금지) | `.env.example`로 키 목록만 공유 |
| 회의록 · 기획 · 일정 · WBS | **Notion / 위키** | 코드와 함께 바뀌지 않는 프로세스 문서 |

---

## 기여 규칙 (문서 작성 시)

- **파일명**: kebab-case. 순서가 있는 문서만 번호 접두사(`01-`, `02-`).
- **ADR**: 번호순(`0001-`, `0002-`)으로 하나의 결정 = 하나의 파일. 배경·결정·대안·트레이드오프를 짧게.
- **문서 헤더**: 각 `.md` 상단에 제목 / 최종 수정일 / 상태(draft·review·done)를 남긴다.
- **동기화**: 코드가 바뀌면 관련 문서를 **같은 PR**에서 갱신한다.
