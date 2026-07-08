# 클라우드 배포 (AWS)

> 최종 수정일: 2026-07-07 · 상태: draft

AWS EC2 3대 + ECR 기반 배포 절차. 물리 토폴로지는 [architecture/05-deployment.md](../architecture/05-deployment.md), CI/CD는 [ci-cd.md](ci-cd.md) 참고. 자원 정의는 [`infra/`](../../infra/README.md).

## 구성 요약

| EC2 | 폴더 | 컨테이너 |
| --- | --- | --- |
| 앱 | `infra/cloud/app/` | nginx · next · app · promtail |
| DB | `infra/cloud/db/` | mysql (자체 호스팅, RDS 아님) |
| 모니터링 | `infra/cloud/monitoring/` | prometheus · loki · grafana |

## 배포 절차

### 0. 사전 (최초 1회)

- EC2 3대 프로비저닝, 같은 VPC 배치. **DB EC2는 퍼블릭 IP 없음**, SG로 앱 EC2에서 3306만 허용.
- ECR 리포지토리(`dongnemarket-app`, `dongnemarket-next`) 생성.
- GitHub Actions용 IAM Role(OIDC) 준비 → `AWS_ROLE_ARN` 시크릿 등록. 자동 배포용 `EC2_APP_HOST`·`EC2_SSH_KEY` 시크릿도 등록 (CI/CD 문서 참고).
- 각 EC2에 Docker/Compose 설치.

### 1. DB EC2

```bash
cd infra/cloud/db
cp .env.example .env      # MYSQL_ROOT_PASSWORD · DB_USERNAME · DB_PASSWORD 채우기
docker compose --env-file .env up -d
```

### 2. 앱 EC2

```bash
cd infra/cloud/app
cp .env.example .env      # 아래 표 참고. DB_URL엔 DB EC2 프라이빗 IP
docker compose --env-file .env pull      # ECR에서 최신 이미지
docker compose --env-file .env up -d
```

앱 EC2 `.env` 키:

| 키 | 설명 |
| --- | --- |
| `ECR_REGISTRY` · `IMAGE_TAG` | pull할 ECR 레지스트리 / 태그(기본 latest) |
| `DB_URL` | `jdbc:mysql://<DB EC2 프라이빗 IP>:3306/dongne_market?...` |
| `DB_USERNAME` · `DB_PASSWORD` | DB EC2와 동일 자격 |
| `JWT_SECRET` | 운영용 시크릿(로컬 기본값 금지) |
| `OLLAMA_BASE_URL` · `OLLAMA_MODEL` | 사내 Ollama 주소·모델 |
| `MAIL_HOST` · `MAIL_PORT` · `MAIL_USERNAME` · `MAIL_PASSWORD` | 이메일 발송(Gmail SMTP) |

> 앱은 `SPRING_PROFILES_ACTIVE=prod`로 뜬다(온프레미스 배포와 공유하는 운영 수위 프로파일 — [ADR 0004](../adr/0004-infra-boundary.md)). `prod`는 아직 `ddl-auto: update`다 — Flyway 도입 후 `validate` 전환 예정([ADR 0003](../adr/0003-schema-ddl-auto.md), P4).

### 3. 모니터링 EC2

```bash
cd infra/cloud/monitoring
cp .env.example .env      # GRAFANA_ADMIN_* 등
docker compose --env-file .env up -d
```

Prometheus가 앱 EC2의 `/actuator/prometheus`를 scrape하고, promtail이 로그를 loki로 보낸다.

## 재배포 (새 이미지 반영)

CD가 develop 머지 시 ECR에 새 이미지를 push하고(태그: `latest` + 커밋 SHA), 이어서 **앱 EC2에 SSH로 접속해 자동 재배포**한다(`docker compose pull` → `up -d` → `image prune`). 상세는 [ci-cd.md](ci-cd.md).

수동으로 반영해야 할 때(핫픽스·롤백 등)는 앱 EC2에서 직접:

```bash
cd infra/cloud/app
docker compose --env-file .env pull && docker compose --env-file .env up -d
```

> 배포는 `IMAGE_TAG=latest` 기준이라 특정 SHA 롤백은 `.env`의 `IMAGE_TAG`를 해당 커밋 SHA로 바꿔 수동 `pull`·`up`으로 처리한다.

## 확인 / 트러블슈팅

| 확인 | 방법 |
| --- | --- |
| 서비스 상태 | 각 EC2에서 `docker compose ps` / `docker compose logs -f <svc>` |
| 앱→DB 연결 실패 | DB EC2 SG(3306), `DB_URL` 프라이빗 IP, 자격증명 확인 |
| 이미지 최신 아님 | `docker compose pull` 후 `up -d`. `IMAGE_TAG` 확인 |
| 메트릭/로그 안 보임 | 모니터링 EC2에서 Prometheus 타겟 up 여부, promtail→loki 연결 확인 |

## 로컬 배포와의 차이

로컬 `local-deploy`는 한 호스트의 docker-compose로 전부 띄운다([runbook/README.md](README.md)). 클라우드는 **역할별 EC2 분리 + ECR 이미지 pull + 프라이빗 DB**라는 점만 다르고, 앱 구조(nginx 현관·프로파일)는 동일하다.
