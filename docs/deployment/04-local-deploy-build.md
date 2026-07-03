# 로컬 배포 환경(local-deploy) 구축·유지보수 기록

이 문서는 "**어떻게 이 환경을 만들었나 + 다음에 어디만 바꾸면 되나**"를 기록한다.
그냥 실행만 하려면 → [03-local-deploy.md](03-local-deploy.md)(실행 런북). 이 문서는 **구축 과정·설계 이유·변경 포인트·함정**에 집중한다.

- **결과물**: 프론트·백엔드·DB·관측·외부노출을 전부 Docker Compose로 띄우는 단일 머신 스택
- **뼈대 원칙**: (1) nginx 단일 현관 → same-origin → **CORS 설정 0** (2) 빌드 산출물은 이미지에 COPY만, 서버 빌드 안 함 (3) 기능을 **compose 프로파일**(`web`/`observability`/`edge`)로 나눠 겹쳐 켬

> 참고: **cloud(AWS) 배포는 개발 완성 후로 보류.** 관련 `application-prod.yml`·`docker-compose.prod.yml`은 제거된 상태.

---

## 0. 전체 구조 (파일 지도)

```
.
├── docker-compose.yml          ★ 루트. 모든 서비스 + 프로파일 정의
├── .env / .env.example         시크릿·계정 (키 목록은 .env.example)
├── nginx/nginx.conf            리버스 프록시 (/ → next, /api → app)
├── monitoring/
│   ├── prometheus.yml          app:8080 메트릭 스크레이프
│   ├── promtail-config.yml     docker.sock 로그 수집 → loki
│   ├── loki-config.yml         로그 저장(단일 바이너리, 파일시스템)
│   └── grafana/provisioning/datasources/datasource.yml   Prometheus+Loki 자동연결
├── backend/
│   ├── Dockerfile              JRE21에 빌드된 jar COPY (서버빌드 X)
│   └── src/main/resources/application-{,dev,local}.yml   프로파일 (부록 A)
└── frontend/
    ├── Dockerfile              Next standalone 3-stage 빌드
    ├── next.config.ts          output:"standalone" + /api rewrites(dev용)
    └── .dockerignore
```

프로파일 매핑:

| 프로파일 | 서비스 | 켜는 법 |
|---|---|---|
| *(없음)* | mysql | `docker compose up -d`(dev용, mysql만) |
| `web` | app · next · nginx | `--profile web` |
| `observability` | prometheus · grafana · loki · promtail | `--profile observability` |
| `edge` | cloudflared | `--profile edge` |

---

## 1단계. 초기 세팅 (뼈대)

**목적**: 모노레포(backend+frontend)를 한 compose로 묶기.

**한 일**
1. compose를 `backend/`에서 **루트로 승격**. 이유: 프론트가 형제 폴더라, 루트에서 `context: ./backend`·`./frontend` 둘 다 빌드하려면 compose가 루트에 있어야 함.
2. 관련 파일도 루트로 이동: `monitoring/`, `.env` / `.env.example`. 옛 `backend/docker-compose.yml` 삭제.
3. `.env` 준비 — 키 목록은 `.env.example` 참조(MySQL 계정·JWT·Grafana 계정).

**변경 포인트**
- 새 서비스 추가 시 → 루트 `docker-compose.yml`에 `profiles: [...]` 붙여 등록.
- 시크릿/계정 → **`.env`만** 수정(compose는 `${VAR}`로 참조).

---

## 2단계. web 프로파일 (nginx + next + app + mysql)

**목적**: 브라우저가 `localhost` 하나로 프론트+API를 쓰게. nginx가 현관.

**만든/바꾼 것**

1. **`frontend/next.config.ts`** — `output: "standalone"` 추가. 이유: 컨테이너에 최소 런타임(`.next/standalone`)만 실어 이미지 경량화. (`/api` rewrites는 dev(호스트)용으로 남겨둠 — 컨테이너에선 nginx가 라우팅하므로 미사용)

2. **`frontend/Dockerfile`** — 3-stage(deps→builder→runner). runner는 `.next/standalone`·`.next/static`·`public`만 COPY 후 `node server.js`. (`package-lock.json` 필요 → `npm ci`)

3. **`nginx/nginx.conf`** — 핵심:
   ```nginx
   resolver 127.0.0.11 valid=10s ipv6=off;      # Docker 내장 DNS
   location /api/ { set $upstream_app app;  proxy_pass http://$upstream_app:8080; ... }
   location /    { set $upstream_next next; proxy_pass http://$upstream_next:3000; ... }
   ```
   > **왜 resolver+변수?** nginx는 upstream 이름을 부팅 시 1회만 DNS 해석한다. 컨테이너 재생성으로 IP가 바뀌면 **502**가 난다(실제로 겪음). 변수 proxy_pass + resolver를 쓰면 요청마다 재해석해 재발을 막는다.

4. **`docker-compose.yml`** web 서비스: `app`(context `./backend`, `SPRING_PROFILES_ACTIVE: local`, `DB_URL=mysql:3306`), `next`(context `./frontend`), `nginx`(`80:80`, nginx.conf 마운트). `mysql`은 무프로파일(항상).

**실행 선행**: `cd backend && ./gradlew clean build -x test` — Dockerfile은 빌드된 jar를 COPY만 하므로 jar가 먼저 있어야 함.

**변경 포인트**
- 프론트 라우팅 규칙 추가(예: `/admin` 별도) → `nginx.conf`에 `location` 추가.
- 앱/프론트 내부 포트 → compose `expose` + nginx.conf upstream 포트.
- 외부 개방 포트(현재 80만) → nginx 서비스 `ports`.

---

## 3단계. observability 프로파일 (Prometheus · Loki · Promtail · Grafana)

**목적**: 지표(메트릭)+로그를 Grafana 한 화면에서.

**만든/바꾼 것**
1. **`monitoring/prometheus.yml`** — `app:8080/actuator/prometheus` 스크레이프. (앱의 actuator prometheus 노출은 `local` 프로파일에서 켬 — 부록 A)
2. **`monitoring/loki-config.yml`** — Loki 3.x 단일 바이너리, 파일시스템 저장.
3. **`monitoring/promtail-config.yml`** — **`docker_sd_configs` + `/var/run/docker.sock`** 로 전 컨테이너 로그 수집.
   > **왜 docker.sock?** Windows/Docker Desktop에선 `/var/lib/docker/containers` 파일 마운트가 잘 안 된다. 소켓 기반 서비스디스커버리로 우회. `service` 라벨 = compose 서비스명.
4. **`.../datasources/datasource.yml`** — Prometheus + **Loki** 자동 프로비저닝.
5. **`docker-compose.yml`** observability 서비스 4개 + `loki-data` 볼륨. **Grafana 호스트 포트는 3001**(컨테이너 3000) — Next dev(3000)와 충돌 회피.

**검증**: `http://localhost:9090/targets`에서 `dongnemarket-app` UP, Grafana Explore에서 `{service="app"}` 로그 조회.

**변경 포인트**
- 로그/메트릭 보존기간 → `loki-config`(retention)·compose prometheus `--storage.tsdb.retention.time`.
- 스크레이프 대상 추가 → `prometheus.yml` `scrape_configs`.
- 대시보드 프로비저닝 → `monitoring/grafana/provisioning/dashboards/`(현재 데이터소스만 자동, 패널은 수동).
- 관측 이미지 버전 → compose의 각 `image:` 태그.

---

## 4단계. edge 프로파일 (Cloudflare 퀵터널)

**목적**: 도메인·계정 없이 **임시 외부 URL**로 시연.

**만든 것** — `docker-compose.yml` `cloudflared`(profile `edge`):
```yaml
command: tunnel --no-autoupdate --url http://nginx:80
```
- 토큰·`.env` 불필요. 터널 끝을 **nginx:80**에 꽂음(현관 재사용).
- 발급 URL: `docker logs dongne-cloudflared 2>&1 | grep trycloudflare`

**변경 포인트 (중요)**
- **고정 도메인이 필요하면** → 퀵터널을 버리고 **Named Tunnel(토큰 방식)**으로 승격: 도메인을 Cloudflare에 등록(NS 이전) → Zero Trust에서 터널 생성·토큰 발급 → command를 `tunnel --no-autoupdate run --token ${CF_TUNNEL_TOKEN}`으로, `.env`에 토큰 추가, public hostname → `http://nginx:80`.
- 관측 UI를 외부에 노출하고 싶지 않다 → 지금처럼 유지(터널은 nginx:80만 태움. Grafana/Prometheus는 로컬 포트라 미노출).

---

## 부록 A. Spring 프로파일 구조

`application.yml`(base)은 **환경 불문 공통만**(앱명·DB placeholder+fallback·jackson·jwt·ollama·springdoc·actuator health,info). 나머지는 프로파일 델타:

| 프로파일 | 파일 | 활성화 | 오버라이드 |
|---|---|---|---|
| `dev` | `application-dev.yml` | `spring.profiles.default: dev`(호스트 bootRun 자동) | ddl:update·SQL debug·+prometheus |
| `local` | `application-local.yml` | compose `SPRING_PROFILES_ACTIVE: local` | ddl:update·로그 quiet·+prometheus |
| `test`/`integration` | test/resources | `@ActiveProfiles` | H2 / Testcontainers |

> `prod`(cloud)는 보류로 제거됨. cloud 재개 시 `application-prod.yml`(validate·시크릿필수·actuator잠금) 재작성.
> 원칙: "환경마다 값이 달라지나?"로 판단 — 공통이면 base(+ env 주입), dev에서만 원하면 dev.yml.

---

## 부록 B. 유지보수 — "이거 바꾸려면 여기"

| 바꾸려는 것 | 위치 |
|---|---|
| DB 계정·비번, JWT, Grafana 계정 | `.env` |
| 외부 개방 포트 | `docker-compose.yml` nginx `ports` |
| 서비스 내부 포트/upstream | compose `expose` + `nginx/nginx.conf` |
| 프론트/API 라우팅 규칙 | `nginx/nginx.conf` `location` |
| 이미지 버전(mysql·nginx·grafana·loki·promtail·prometheus·cloudflared) | compose 각 `image:` 태그 |
| 로그/메트릭 보존기간 | `monitoring/loki-config.yml`, compose prometheus `retention.time` |
| 스크레이프 대상 추가 | `monitoring/prometheus.yml` |
| dev/local 동작(ddl·로그·actuator) | `application-dev.yml` / `application-local.yml` |
| Ollama 주소 | base `application.yml`의 `OLLAMA_BASE_URL`(env 주입 가능) |
| 고정 도메인(퀵터널→Named) | 4단계 변경 포인트 참조 |

---

## 부록 C. 구축 중 겪은 함정

| 증상 | 원인 | 해결 |
|---|---|---|
| `container name "/dongne-xxx" is already in use` | 옛 컨테이너(구 backend compose) 잔재 | `docker compose down --remove-orphans` (안 되면 `docker rm -f $(docker ps -aq --filter name=dongne-)`) |
| `/api` **502**(로컬·터널 둘 다) | nginx가 재생성된 app의 옛 IP 캐시 | `nginx.conf` resolver+변수 proxy_pass(반영됨). 즉시엔 `docker restart dongne-nginx` |
| app 이미지 빌드 실패(jar 없음) | Dockerfile은 jar COPY만 | `./gradlew clean build -x test` 선행 |
| Grafana 포트 충돌 | 컨테이너 3000이 next dev 3000과 겹침 | 호스트 포트 3001로 매핑 |
| Promtail 로그 못 읽음 | Windows에서 `/var/lib/docker/containers` 마운트 실패 | `docker_sd_configs` + `docker.sock` 사용 |
| 프로파일 오버라이드 미적용 | 파일명 `application-operator.yml` ≠ 활성 `operate` | 프로파일 어휘 `local`로 통일 |
