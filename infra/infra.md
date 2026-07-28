# infra — 배포 자원

컨테이너 배포 자원. **dev(매일 개발)** 는 리포 루트 [`docker-compose.yml`](../docker-compose.yml)(MySQL만)이 담당하고, **전체 스택 배포**는 배포 지형별로 이 폴더에 나뉜다.

> 이 문서는 `infra/`를 이해하는 진입점이다. 리포 전체는 [../README.md](../README.md).

## 구조

| 폴더 | 대상 |
|---|---|
| `onprem/` | **온프레미스 전체 스택** — nginx · Next · Spring Boot · MySQL 한 호스트, 이미지 로컬 빌드 |
| `cloud/app/` | 앱 EC2 — nginx · Next.js · Spring Boot (ECR pull, DB 연결) |
| `cloud/db/` | DB EC2 — MySQL 8 컨테이너 (관리형 RDS 대신 EC2 직접 호스팅) |
| `cloud/monitoring/` | 모니터링 EC2 — Prometheus · Loki · Grafana |

`onprem`과 `cloud`는 **같은 앱 이미지·같은 nginx 라우팅**을 쓴다. 차이는 프로파일이 아니라 env와 이미지 출처뿐이다.

| | onprem | cloud |
|---|---|---|
| 이미지 | 배포 호스트에서 로컬 빌드 | GitHub Actions가 ECR로 push → EC2가 pull |
| 파일 저장 | `FILE_STORAGE_TYPE=local` | `s3` |

Dockerfile은 각 앱 폴더(`backend/`, `frontend/`)에 있으며 **지형 무관 공용**이다.

## 기동

```bash
# 각 지형 폴더(onprem/ 또는 cloud/<역할>/)에서:
cp .env.example .env                    # 값 채우기. .env 는 커밋 금지
docker compose --env-file .env up -d    # onprem: 로컬 빌드 / cloud: 먼저 pull
```

### 온프레미스 — 프로파일

```bash
cd backend && ./gradlew clean build -x test && cd ..   # 앱 이미지용 JAR 선행 빌드(필수)

cd infra/onprem
docker compose --env-file .env up -d --build                                          # nginx+next+app+mysql
docker compose --env-file .env --profile observability up -d --build                  # +관측
docker compose --env-file .env --profile observability --profile edge up -d --build   # +외부노출(퀵터널)
```

접속 지점:

| 대상 | 주소 |
|---|---|
| 프론트 / API | http://localhost · http://localhost/api/... |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |
| 외부 임시 URL | `docker logs dongne-cloudflared 2>&1 \| grep trycloudflare` |

종료 (데이터 볼륨은 유지):

```bash
cd infra/onprem && docker compose --profile observability --profile edge down
```

## CI/CD

- **CI** (`.github/workflows/ci.yml`) — feature 브랜치 push + develop 대상 PR에서 백엔드 테스트·프론트 체크·nginx 설정 동기화 검사.
- **CD** (`.github/workflows/cd-app.yml`) — develop push 시 변경된 앱 이미지를 빌드해 ECR로 push하고, 앱 EC2에 SSH로 접속해 pull·재기동.
- AWS 인증은 **OIDC**(`secrets.AWS_ROLE_ARN`). 배포 대상은 `secrets.EC2_APP_HOST`, 접속 키는 `secrets.EC2_SSH_KEY`.

> ⚠️ 리포를 이전하면 **IAM 역할의 신뢰 정책에 새 리포 경로를 허용**해야 한다. 안 하면 모든 배포가 OIDC 인증에서 실패한다.

## 스키마

운영 스키마는 **Flyway 마이그레이션**으로 관리하고 `ddl-auto: validate`로 검증한다. 마이그레이션 파일은 `backend/src/main/resources/db/migration/`.

## 주의

- `.env`는 커밋하지 않는다(gitignore). `.env.example`이 필요한 키 목록이다.
- 관측 스택은 **클라우드 전용**이다. 로컬 관측은 온프레미스의 `observability` 프로파일로 띄운다.
- 배포 구성이 바뀌면 이 문서를 **같은 PR에서** 갱신한다.
