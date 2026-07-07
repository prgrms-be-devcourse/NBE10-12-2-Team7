# 운영 런북 (runbook)

> 최종 수정일: 2026-07-07 · 상태: draft

배포·운영·장애 대응 절차. 로컬 실행 자체는 [getting-started](../getting-started/README.md)를, 시스템 구성은 [architecture](../architecture/README.md)를 본다.

| 문서 | 내용 |
| --- | --- |
| (이 문서) | local-deploy 운영·관측·장애 대응 |
| [cloud-deploy.md](cloud-deploy.md) | AWS EC2 3대 배포 절차 |
| [ci-cd.md](ci-cd.md) | GitHub Actions CI/CD 파이프라인 |

## 1. 배포 형태

| 환경 | 실행 주체 | 정의 위치 |
| --- | --- | --- |
| **local-deploy** (로컬 전체 컨테이너) | 개발자 PC | 루트 [`docker-compose.yml`](../../docker-compose.yml) |
| **AWS 클라우드** (앱·DB·모니터링 EC2 3대) | GitHub Actions → ECR → EC2 pull | [`infra/`](../../infra/README.md) · 절차 [cloud-deploy.md](cloud-deploy.md) |

### local-deploy 프로파일

```bash
cd backend && ./gradlew clean build -x test && cd ..              # 앱 JAR 선행 빌드(필수)
docker compose --profile web up -d --build                        # nginx+next+app+mysql
docker compose --profile web --profile observability up -d --build # +관측
docker compose --profile web --profile observability --profile edge up -d  # +외부 임시 URL
```

→ http://localhost (nginx 현관). 종료: `docker compose --profile web --profile observability --profile edge down` (데이터 볼륨 유지).

### AWS 클라우드 배포 (개요)

- **이미지**: GitHub Actions가 `backend/`·`frontend/` 이미지를 빌드해 **ECR로 push**. EC2는 **pull만** 한다.
- **구성**: 앱 EC2(nginx·Next·Spring Boot), DB EC2(MySQL 8 컨테이너), 모니터링 EC2(Prometheus·Loki·Grafana).
- 각 EC2에는 `infra/cloud/<역할>/` 폴더만 올리고 그 안에서 `docker compose --env-file .env up -d`.
- **상세 배포 절차는 [cloud-deploy.md](cloud-deploy.md), CI/CD는 [ci-cd.md](ci-cd.md), 토폴로지는 [architecture/05-deployment.md](../architecture/05-deployment.md).**

## 2. 관측 (Observability)

`observability` 프로파일로 기동 시 접근:

| 도구 | 로컬 주소 | 용도 |
| --- | --- | --- |
| Grafana | http://localhost:3001 | 메트릭·로그 통합 대시보드 |
| Prometheus | http://localhost:9090 | 앱 메트릭 수집(`/actuator/prometheus`, 7d 보존) |
| Loki | http://localhost:3100 | 로그 저장 (promtail이 컨테이너 로그 수집) |

설정 파일: [`monitoring/`](../../monitoring/) (`prometheus.yml` · `loki-config.yml` · `promtail-config.yml`).

## 3. 자주 하는 운영 작업

```bash
docker compose ps                          # 컨테이너 상태
docker compose logs -f app                 # 앱 로그 팔로우 (nginx/next/mysql도 동일)
docker compose restart app                 # 앱만 재시작
docker compose up -d --build app           # 앱 이미지 재빌드 후 교체
docker logs dongne-cloudflared 2>&1 | grep trycloudflare   # 임시 외부 URL 확인(edge)
```

- **영속 볼륨**: `dongne-mysql-data`(DB), `dongne-report-evidence`(신고 증빙 이미지)는 `down` 후에도 유지된다. 완전 초기화는 `docker compose down -v` (⚠️ 데이터 삭제).

## 4. 장애 대응 기본

| 증상 | 1차 확인 |
| --- | --- |
| API 5xx / 앱 다운 | `docker compose logs app` → 스택트레이스. DB 연결·마이그레이션 여부 확인 |
| DB 연결 실패 | mysql 컨테이너 `healthy` 인지(`docker compose ps`). 자격증명(`.env`) 확인 |
| 프론트만 안 뜸 | next 컨테이너 로그, nginx 프록시 설정(`nginx/nginx.conf`) 확인 |
| develop 머지 후 깨짐 | 협업 규칙대로 PR 작성자 우선 복구, 지연 시 해당 머지 revert 후 재PR ([conventions](../conventions/git-collaboration.md) §9) |

## 5. 미정 / 후속

- **자동 배포**: 현재 CD는 ECR push까지만. EC2 pull·재기동은 수동이다([ci-cd.md](ci-cd.md)). SSH/SSM 기반 자동 배포 job 추가가 후속.
- **DB 백업·복구 정책**: EC2 자체 호스팅 MySQL이라 스냅샷/백업을 직접 세워야 한다([ADR 0002](../adr/0002-db-hosting-ec2-mysql.md)).
- **AWS 캐패시티 플래닝**: 실서버 부하 기준 인스턴스 사이징 문서 미작성.
