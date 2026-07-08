# infra — 배포 자원

컨테이너 배포 자원. **dev(매일 개발)** 는 리포 루트 [`docker-compose.yml`](../docker-compose.yml)(MySQL만)이 담당하고,
**전체 스택 배포**는 배포 지형별로 이 폴더에 나뉜다. 경계 설계는 [ADR 0004](../docs/adr/0004-infra-boundary.md),
배포·운영 절차는 [docs/runbook/README.md](../docs/runbook/README.md).

## 구조

- `onprem/`           → **온프레미스 전체 스택** (nginx · Next · Spring Boot · MySQL 한 호스트, 이미지 로컬 빌드)
- `cloud/app/`        → 앱 EC2 (nginx · Next.js · Spring Boot, ECR pull, DB 연결)
- `cloud/db/`         → DB EC2 (MySQL 8 컨테이너) ※ 관리형 RDS 대신 EC2 직접 호스팅
- `cloud/monitoring/` → 모니터링 EC2 (Prometheus · Loki · Grafana)

**onprem**과 **cloud**는 같은 앱 이미지·같은 nginx 라우팅을 쓰되, 차이는 프로파일이 아니라
env·이미지 출처뿐이다(onprem=로컬 빌드+`FILE_STORAGE_TYPE=local`, cloud=ECR pull+`s3`).

```bash
# 각 지형 폴더(onprem/ 또는 cloud/<역할>/)에서:
cp .env.example .env      # 값 채우기. .env 는 커밋 금지.
docker compose --env-file .env up -d          # onprem: 로컬 빌드 / cloud: 먼저 pull
```

## 이미지

**cloud**: 앱/프론트 이미지는 GitHub Actions가 빌드해 ECR로 push한다(EC2는 pull만).
**onprem**: 같은 Dockerfile로 배포 호스트에서 직접 빌드한다.
Dockerfile은 각 앱 폴더(`backend/`, `frontend/`)에 있으며 지형 무관 공용이다.
