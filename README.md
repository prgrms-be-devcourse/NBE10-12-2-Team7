# 마켓온

지역 기반 중고거래 서비스. **모노레포**(Spring Boot 백엔드 + Next.js 프론트 + 인프라).

> 처음이라면 이 문서로 **띄우고**, 팀 협업 방식은 [docs/README.md](docs/README.md), 배포는 [docs/deployment](docs/deployment/README.md)를 본다.

---

## 폴더 구조

```
.
├── backend/            Spring Boot (Java 21) · build.gradle · src
├── frontend/           Next.js (App Router, TS) · Dockerfile
├── nginx/              로컬 배포용 리버스 프록시 설정
├── monitoring/         Prometheus · Grafana · Loki · Promtail 설정
├── docker-compose.yml  ★ 루트에서 실행 (dev / local-deploy 통합)
├── .env.example        필요한 환경변수 키 목록 (복사해서 .env)
└── docs/               협업·아키텍처·배포 문서
```

## 기술 스택

| | |
|---|---|
| 백엔드 | Spring Boot 3.5, Java 21, Spring Security(JWT), JPA, MySQL 8 |
| 프론트 | Next.js 16(App Router), React 19, TypeScript, Tailwind |
| 인프라 | Docker Compose, nginx, Prometheus·Loki·Grafana, Cloudflare Tunnel |
| AI | Spring AI + 사내 Ollama (관리자 AI 어시스턴트) |

---

## 빠른 시작

> 전제: **Docker Desktop**, **JDK 21**. 모든 명령은 **리포 루트**에서. 먼저 `cp .env.example .env`.

### A. dev — 매일 개발 (권장, 빠른 루프)

MySQL만 Docker, 앱·프론트는 호스트에서 직접 실행:

```bash
docker compose up -d --wait          # MySQL만 (무프로파일)
cd backend && ./gradlew bootRun      # 백엔드 :8080
cd frontend && npm install && npm run dev   # 프론트 :3000 (/api는 :8080으로 프록시)
```

→ 브라우저 **http://localhost:3000**

### B. local-deploy — 전부 Docker로 (운영 패리티·시연·공유)

프론트·백엔드·DB·관측까지 컨테이너로. **자세한 절차·트러블슈팅은 → [docs/deployment/03-local-deploy.md](docs/deployment/03-local-deploy.md)**

```bash
cd backend && ./gradlew clean build -x test && cd ..   # 앱 이미지용 JAR 선행 빌드(필수)

docker compose --profile web up -d --build                              # 앱만
docker compose --profile web --profile observability up -d --build      # +관측
docker compose --profile web --profile observability --profile edge up -d --build  # +외부노출(퀵터널)
```

→ 브라우저 **http://localhost** (nginx 현관 하나로 프론트·API 통합)

### 접속 지점 (local-deploy)

| 대상 | 주소 |
|---|---|
| 프론트 / API | http://localhost · http://localhost/api/... |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |
| 외부 임시 URL | `docker logs dongne-cloudflared 2>&1 \| grep trycloudflare` |

### 종료

```bash
docker compose --profile web --profile observability --profile edge down   # 데이터 볼륨은 유지
```

---

## 문서

| 문서 | 내용 |
|---|---|
| [docs/README.md](docs/README.md) | 팀 협업 방식(AI Native), 역할 분담, 개발 흐름 |
| [docs/architecture](docs/architecture) | 시스템 구조(C4), ERD, 주요 흐름 |
| [docs/deployment](docs/deployment/README.md) | 배포 — [로컬 배포 런북](docs/deployment/03-local-deploy.md), AWS 캐패시티 |

---

## 참고

- **CORS 설정 없음**: 어느 환경이든 브라우저는 단일 origin(dev=Next, local-deploy=nginx)만 호출하고 `/api`는 서버가 프록시한다.
- `.env`는 커밋하지 않는다(gitignore). `.env.example`이 필요한 키 목록.
