# 시작하기 (getting-started)

신규 투입자가 **로컬에서 마켓온을 처음 띄우기까지**의 가이드. 전체 실행 옵션의 정본은 루트 [README.md](../../README.md)이며, 이 문서는 "첫날 순서대로 따라 하기"에 초점을 둔다.

> 최종 수정일: 2026-07-07 · 상태: draft

## 1. 전제 조건

| 도구 | 버전 | 용도 |
| --- | --- | --- |
| **Docker Desktop** | 최신 | MySQL·(선택)전체 컨테이너 실행 |
| **JDK** | **21** | 백엔드(Spring Boot) 빌드·실행 |
| **Node.js** | 20 LTS+ | 프론트(Next.js 16) 실행 |
| Git | 최신 | 소스 관리 |

> 백엔드는 `./gradlew`(Gradle Wrapper)를 쓰므로 Gradle 별도 설치는 불필요하다.

## 2. 클론 & 환경변수

```bash
git clone <repo-url>
cd pc3-marketon
cp .env.example .env      # 필요한 키 목록이 담겨 있다
```

`.env`에 채우는 값:

| 키 | 설명 | 로컬 기본값 유무 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` · `DB_USERNAME` · `DB_PASSWORD` | MySQL 접속 정보 | compose에 기본값 있음(로컬은 그대로 가능) |
| `JWT_SECRET` | JWT 서명 키 | 로컬 기본값 있음(운영은 반드시 교체) |
| `MAIL_USERNAME` · `MAIL_PASSWORD` | Gmail SMTP 계정 — 이메일 인증·비밀번호 재설정 발송 | **기본값 없음.** 메일 기능을 쓸 때만 채운다 |
| `GRAFANA_ADMIN_USER` · `GRAFANA_ADMIN_PASSWORD` | Grafana 로그인 | 온프레미스 `infra/onprem/.env`에서만(observability 프로파일) |

> `.env`는 커밋하지 않는다(gitignore). 메일 계정 등 비밀은 개인이 관리한다.

## 3. 실행 — 두 가지 모드

전제: 모든 명령은 **리포 루트**에서. Docker Desktop 실행 중이어야 한다.

### A. dev — 매일 개발 (권장, 빠른 루프)

MySQL만 Docker로, 앱·프론트는 호스트에서 직접 실행한다.

```bash
docker compose up -d --wait                    # MySQL만 기동
cd backend && ./gradlew bootRun                # 백엔드 :8080
cd frontend && npm install && npm run dev      # 프론트 :3000 (/api는 :8080으로 프록시)
```

→ 브라우저 **http://localhost:3000**

### B. 온프레미스 — 전부 Docker (운영 패리티·시연)

정의는 [`infra/onprem/`](../../infra/onprem/) 폴더에 있다.

```bash
cd backend && ./gradlew clean build -x test && cd ..   # 앱 이미지용 JAR 선행 빌드(필수)
cd infra/onprem && cp .env.example .env                # 최초 1회
docker compose --env-file .env up -d --build           # nginx+next+app+mysql
```

→ 브라우저 **http://localhost** (nginx 현관 하나로 프론트·API 통합)

> 관측(Grafana 등)·외부노출은 프로파일(`--profile observability`/`--profile edge`)을 추가한다. 상세·트러블슈팅은 루트 [README.md](../../README.md).

## 4. 잘 떴는지 확인

| 확인 | 주소 (dev 기준) |
| --- | --- |
| 프론트 | http://localhost:3000 |
| API 헬스 | http://localhost:8080/actuator/health |
| **Swagger UI** (API 문서) | http://localhost:8080/swagger-ui.html |

- **시드 데이터**: 앱 최초 기동 시 카테고리·기본 데이터와 **관리자 계정 1개**가 자동 생성된다.
  로컬 관리자: `admin@dongnemarket.com` / `admin1234!` (로컬 시드 전용 — 운영과 무관).
- 온프레미스 모드에서는 위 주소의 포트 대신 **http://localhost** 및 `http://localhost/api/...` 로 접근한다.

## 5. 자주 겪는 문제

| 증상 | 원인 / 해결 |
| --- | --- |
| 백엔드가 DB 연결 실패 | MySQL 컨테이너가 아직 안 떴을 수 있음 → `docker compose up -d --wait` 로 healthy 대기 후 실행 |
| 포트 충돌(3000/3306/8080) | 기존 프로세스 종료 또는 compose 포트 매핑 확인. Grafana는 next와 겹쳐 호스트 `3001` 사용 |
| 이메일 인증이 안 됨 | `.env`의 `MAIL_USERNAME`/`MAIL_PASSWORD` 미설정. 메일 기능 안 쓰면 무시 가능 |
| `gradlew` 권한/실행 오류 | Windows는 `gradlew.bat`, macOS/Linux는 `./gradlew` 사용 |
| 종료 | dev: `docker compose down` / 온프레미스: `cd infra/onprem && docker compose down` (데이터 볼륨은 유지) |

## 6. 다음으로

- 시스템 구조 이해 → [architecture](../architecture/README.md) (컨텍스트·컨테이너·컴포넌트·ERD)
- 협업 규칙(브랜치·PR·개발 흐름) → [conventions/git-collaboration.md](../conventions/git-collaboration.md)
- 왜 이렇게 정했나 → [adr](../adr/README.md)
