# infra — 클라우드 배포 자원

AWS(EC2×2 + RDS + ECR) 클라우드 배포 전용 자원. dev/local-deploy는 리포 루트의
`docker-compose.yml`이 담당하며 여기서 다루지 않는다. 배포·운영 절차는
[docs/runbook/README.md](../docs/runbook/README.md).

## 구조

- `cloud/app/`        → 앱 EC2 (nginx · Next.js · Spring Boot, ECR pull, DB 연결)
- `cloud/db/`         → DB EC2 (MySQL 8 컨테이너) ※ 관리형 RDS 대신 EC2 직접 호스팅
- `cloud/monitoring/` → 모니터링 EC2 (Prometheus · Loki · Grafana)  ※ 이후 단계에서 추가

각 EC2에는 **해당 하위 폴더만** 올리고, 그 안에서 실행한다.

```bash
cp .env.example .env      # 값 채우기 (RDS·ECR·시크릿). .env 는 커밋 금지.
docker compose --env-file .env pull
docker compose --env-file .env up -d
```

## 이미지

앱/프론트 이미지는 GitHub Actions가 빌드해 ECR로 push한다(EC2는 pull만).
Dockerfile은 각 앱 폴더(`backend/`, `frontend/`)에 있으며 환경 무관 공용이다.
