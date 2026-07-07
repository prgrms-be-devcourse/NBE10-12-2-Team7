# C4 L2 — 컨테이너

> 최종 수정일: 2026-07-07 · 상태: draft

배포 단위(컨테이너)와 통신 경로. 실체는 루트 [`docker-compose.yml`](../../docker-compose.yml)이며, 프로파일로 계층을 켠다.

```mermaid
graph TB
    browser["👤 브라우저"]

    subgraph web["profile: web (기본 앱 계층)"]
        nginx["nginx :80<br/>단일 현관·리버스 프록시"]
        next["next :3000<br/>Next.js 16 프론트"]
        app["app :8080<br/>Spring Boot 3.5 / Java 21"]
        mysql[("mysql :3306<br/>MySQL 8 · dongne_market")]
    end

    subgraph obs["profile: observability (선택)"]
        promtail["promtail<br/>컨테이너 로그 수집"]
        loki["loki :3100<br/>로그 저장"]
        prometheus["prometheus :9090<br/>메트릭 수집"]
        grafana["grafana :3001<br/>대시보드"]
    end

    gmail["✉️ Gmail SMTP"]
    ollama["🤖 사내 Ollama"]
    cf["☁️ cloudflared (profile: edge)"]

    browser -->|"HTTP :80"| nginx
    nginx -->|"/ (정적·SSR)"| next
    nginx -->|"/api → 프록시"| app
    app -->|"JDBC"| mysql
    app -->|"SMTP"| gmail
    app -->|"Spring AI"| ollama

    app -.->|"/actuator/prometheus"| prometheus
    promtail -->|"push"| loki
    prometheus --> grafana
    loki --> grafana

    cf -.->|"tunnel → nginx:80"| nginx
```

## 컨테이너 목록

| 컨테이너 | 이미지 | 포트 | 프로파일 | 역할 |
| --- | --- | --- | --- | --- |
| **nginx** | `nginx:1.27-alpine` | `80:80` | web | 단일 origin 현관. `/`→next, `/api`→app 프록시 |
| **next** | (빌드) `frontend/` | expose 3000 | web | Next.js 16 프론트 (App Router, SSR) |
| **app** | (빌드) `backend/` | expose 8080 | web | Spring Boot REST API |
| **mysql** | `mysql:8.0` | `3306:3306` | (기본) | 데이터 저장. DB `dongne_market`, utf8mb4 |
| **prometheus** | `prom/prometheus:v2.53.2` | `9090` | observability | 메트릭 수집(7d 보존) |
| **grafana** | `grafana/grafana:11.1.4` | `3001:3000` | observability | 메트릭·로그 대시보드 |
| **loki** | `grafana/loki:3.1.1` | `3100` | observability | 로그 저장 |
| **promtail** | `grafana/promtail:3.1.1` | — | observability | 컨테이너 로그 → loki |
| **cloudflared** | `cloudflare/cloudflared` | — | edge | 임시 외부 URL(Quick Tunnel) |

## 실행 모드 (프로파일 조합)

| 모드 | 명령 | 구성 |
| --- | --- | --- |
| **dev** (매일 개발) | `docker compose up -d --wait` | mysql만 컨테이너, app·next는 호스트에서 직접 실행 |
| **local-deploy** | `docker compose --profile web up -d --build` | nginx+next+app+mysql |
| **+관측** | `... --profile web --profile observability ...` | 위 + Prometheus·Loki·Grafana |
| **+외부노출** | `... --profile edge ...` | 위 + Cloudflare 퀵터널 |

> 실행 절차의 상세는 루트 [README.md](../../README.md) 참고.

## 핵심 설계 결정

- **단일 origin (nginx 현관 하나)**: 브라우저는 항상 한 origin만 호출하고 `/api`는 nginx가 프록시한다. 그래서 **CORS 설정이 없다**. dev 모드에서는 Next dev 서버가 같은 역할(`/api`→:8080 프록시)을 한다.
- **app은 외부 미노출**: `expose`만 하고 `ports` 매핑 없음 → nginx 통해서만 접근.
- **관측·엣지는 선택 프로파일**: 개발 기본 경로를 가볍게 유지.
- **영속 볼륨**: mysql 데이터, 신고 증빙 이미지(`report-evidence`)는 볼륨으로 컨테이너 재시작에도 유지.

> 백엔드 내부(도메인/계층)는 [03-component.md](03-component.md) 참고.
