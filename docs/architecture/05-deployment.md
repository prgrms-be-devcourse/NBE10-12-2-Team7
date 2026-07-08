# 배포 토폴로지 (AWS)

> 최종 수정일: 2026-07-07 · 상태: draft

로컬(docker-compose)이 아닌 **AWS 클라우드 배포**의 물리 구성. 로컬 컨테이너 구성은 [02-container.md](02-container.md), 배포 절차는 [runbook/cloud-deploy.md](../runbook/cloud-deploy.md) 참고.

```mermaid
graph TB
    user["👤 사용자"]
    gha["GitHub Actions<br/>(CD: 빌드·push·배포)"]
    ecr["Amazon ECR<br/>app · next 이미지"]
    ollama["🤖 사내 Ollama<br/>10.111.111.90:11434"]

    subgraph vpc["AWS VPC"]
        subgraph appec2["앱 EC2"]
            nginx["nginx :80"]
            next["next :3000"]
            app["app :8080 (Spring Boot)"]
            promtail1["promtail"]
        end
        subgraph dbec2["DB EC2"]
            mysql[("MySQL 8 컨테이너<br/>:3306 (프라이빗)")]
        end
        subgraph monec2["모니터링 EC2"]
            prom["Prometheus"]
            loki["Loki"]
            grafana["Grafana"]
        end
    end

    user -->|"HTTP"| nginx
    nginx --> next
    nginx -->|"/api"| app
    app -->|"JDBC · 프라이빗 IP · SG 허용"| mysql
    app -->|"Spring AI"| ollama
    gha -->|"OIDC push"| ecr
    gha -->|"SSH: pull·up"| appec2
    ecr -.->|"pull"| appec2
    prom -->|"scrape /actuator/prometheus"| app
    promtail1 -->|"로그 push"| loki
    prom --> grafana
    loki --> grafana
```

## EC2 3대 구성

| EC2 | 컨테이너 | 폴더 | 역할 |
| --- | --- | --- | --- |
| **앱 EC2** | nginx · next · app · promtail | [`infra/cloud/app/`](../../infra/cloud/app/) | 사용자 트래픽 처리. 이미지는 ECR pull |
| **DB EC2** | mysql | [`infra/cloud/db/`](../../infra/cloud/db/) | **MySQL 8 컨테이너 자체 호스팅** (관리형 RDS 아님 — [ADR 0002](../adr/0002-db-hosting-ec2-mysql.md)) |
| **모니터링 EC2** | prometheus · loki · grafana | [`infra/cloud/monitoring/`](../../infra/cloud/monitoring/) | 앱 메트릭 scrape, 로그 수집, 대시보드 |

각 EC2에는 `infra/cloud/<역할>/` 폴더만 올리고 그 안에서 `docker compose --env-file .env up -d`.

## 네트워킹

- **단일 VPC** 안에 3대. **DB EC2는 퍼블릭 IP 없이** 같은 VPC의 앱 EC2만 **Security Group**으로 3306 허용.
- 앱은 `DB_URL`에 **DB EC2의 프라이빗 IP**를 넣어 접속.
- 사용자 진입은 앱 EC2의 nginx(:80) 단일 현관 → `/`는 next, `/api`는 app (로컬과 동일 구조, CORS 없음).

## 이미지 파이프라인

- GitHub Actions가 `backend/`·`frontend/` 이미지를 빌드해 **ECR로 push**(OIDC 인증)하고, 이어서 **앱 EC2에 SSH로 접속해 pull·재기동**까지 자동화한다.
- 상세는 [runbook/ci-cd.md](../runbook/ci-cd.md).

## as-built 특이사항 (주의)

- **앱 EC2도 `SPRING_PROFILES_ACTIVE=local`** 로 기동한다 — 별도 prod 프로파일이 없어 로컬 프로파일을 재사용한다. 이 프로파일은 `ddl-auto: update`라 **운영 스키마가 Hibernate 자동변경**된다. 관련 위험·후속은 [ADR 0003](../adr/0003-schema-ddl-auto.md).
- **ECR push 이후 앱 EC2 배포는 CD가 SSH로 자동화**(pull·재기동)한다. 단 `:latest` 태그 기준이라 특정 SHA 롤백·헬스체크는 아직 수동이다.
