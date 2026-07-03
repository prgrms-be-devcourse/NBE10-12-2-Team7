# 관측(Observability) 스택 도입 가이드

> 대상: 동네마켓(dongne-market) 팀 전원 · 상태: 0단계(문서화) 완료 → 1단계(앱 메트릭 노출) 착수 직전
> 성격: **개념 설명 + 실행 런북**. 이 문서 하나로 "왜 쓰는지"와 "돌아갈 때 무엇을 보고 어떻게 조치하는지"를 모두 다룬다.

---

## 목차

- [0. 이 문서는 (+ 독자별 읽는 법)](#0-이-문서는)
- [1. 빠른 시작 (TL;DR)](#1-빠른-시작-tldr)
- [2. 왜 도입하는가 (우리 프로젝트 시나리오)](#2-왜-도입하는가)
- [3. 도구별 역할](#3-도구별-역할)
- [4. 아키텍처 — 우리 스택 기준](#4-아키텍처--우리-스택-기준)
- [5. 도입 로드맵](#5-도입-로드맵)
- [6. 1단계 상세 — 앱 메트릭 노출](#6-1단계-상세--앱-메트릭-노출)
- [7. 돌렸을 때: 무엇을 어디서 보나](#7-돌렸을-때-무엇을-어디서-보나)
- [8. 트러블슈팅](#8-트러블슈팅)
- [9. 장애 대응 런북 (증상 → 확인 → 조치)](#9-장애-대응-런북)
- [10. 팀 컨벤션](#10-팀-컨벤션-반드시-지킬-것)
- [11. 용어 정리](#11-용어-정리)
- [12. 진행 체크리스트](#12-진행-체크리스트)

---

## 0. 이 문서는

동네마켓 백엔드에 **Prometheus·Loki·Grafana 기반 관측 스택**을 로컬 환경에 단계적으로 도입하기 위한 가이드이자 실행 런북이다. 운영/관리 담당(팀장)이 세 도구를 처음 학습하며 적용하는 과정을 기록하고, 다른 팀원이 **같은 환경을 재현**하고 **시스템이 돌아갈 때 스스로 확인·조치**할 수 있게 하는 것이 목적이다.

**우리 스택 전제**: Java 21 · Spring Boot 3.5.15 (Spring MVC, 내장 Tomcat) · Spring Data JPA(Hibernate) · Spring Security + JWT · Spring AI(로컬 Ollama, VPN 내부) · MySQL 8.0 · 프론트 Next.js(별도 `frontend/`) · 구동 `./gradlew bootRun`(호스트 직접 실행), MySQL은 `docker compose up -d`

**규모**: 로컬 단일 인스턴스(개발 PC) · 학습/포트폴리오 목적 · **이번 도입 범위는 로컬 한정**(배포 서버 적용 제외)

> ⚠️ 배포(운영) 서버 적용은 이번 문서 범위가 아니다. 프리티어 EC2 1GB RAM 제약상 3개 스택을 운영 서버에 함께 올릴 수 없다. 운영 반영은 로컬 학습 완료 후 별도 논의(Grafana Cloud 경량 에이전트 등). 관련: [docs/deployment](../deployment/README.md)

### 독자별 읽는 법 (여기부터 보세요)

| 당신이 | 먼저 볼 곳 |
|--------|-----------|
| 이걸 **왜 하는지** 궁금한 팀원 | §2 왜 도입하는가 |
| 처음으로 **스택을 띄워보는** 팀원 | §1 빠른 시작 → §6 1단계 상세 |
| 시스템이 켜진 상태에서 **상태를 보고 싶은** 사람 | §7 무엇을 어디서 보나 |
| 뭔가 **안 될 때** | §8 트러블슈팅 |
| **느리다/에러난다** 신고를 받은 사람 | §9 장애 대응 런북 |

---

## 1. 빠른 시작 (TL;DR)

> 각 명령이 **어느 단계에서 유효한지** 표시했다. 아직 안 만든 단계의 명령은 해당 단계 완료 후 동작한다.

```bash
# ── 전제: 저장소 루트에서 시작 ──────────────────────────
# 1) DB 띄우기 (필수, 항상)
docker compose up -d                       # MySQL 8.0 (backend/docker-compose.yml)

# 2) 앱 실행 → 메트릭 노출 확인            [1단계부터 유효]
cd backend && ./gradlew bootRun
curl http://localhost:8080/actuator/prometheus | head

# 3) 관측 스택 띄우기                       [2단계 이후 유효]
cd monitoring && docker compose up -d      # prometheus + grafana (+ loki, alloy)

# 4) 브라우저로 접속
#   Grafana     http://localhost:3000   (초기 admin / .env 참조)   [3단계 이후]
#   Prometheus  http://localhost:9090   (Status → Targets 확인)    [2단계 이후]
```

**동작 순서 요지**: MySQL → 앱(bootRun) → Prometheus가 앱의 `/actuator/prometheus`를 15초마다 긁음 → Grafana가 Prometheus·Loki를 붙여 대시보드로 표시.

---

## 2. 왜 도입하는가

### 현재 방식의 한계 (지금 우리가 겪는 것)

- 앱 상태를 아는 유일한 창은 **`bootRun` 터미널 로그**뿐이다. 터미널을 닫거나 앱이 재시작되면 그 이력은 사라진다(휘발성).
- "지금 힙이 얼마 남았나, DB 커넥션 풀이 고갈됐나, 어떤 API가 느린가?"를 **숫자로 볼 방법이 없다.** 느낌으로만 판단한다.
- 에러가 나면 긴 콘솔 로그를 스크롤로 뒤져야 하고, **시간대·요청·레벨로 필터링**할 수 없다.
- 지표와 로그가 분리돼 있어 "느려진 그 순간의 로그"를 맞춰 보기 어렵다.

### 우리 프로젝트에서 실제로 아플 지점 (구체 시나리오)

이 세 가지는 우리 코드베이스 특성상 **관측이 없으면 원인 파악이 특히 어려운** 케이스다. 팀원 설득용으로 이 예시를 쓰면 된다.

1. **관리자 AI가 먹통** — `/api/admin/ai/chat`은 VPN 내부의 원격 Ollama(`10.111.111.90`)에 의존한다([AdminAiConfig.java](../../backend/src/main/java/com/dongnemarket/admin/ai/config/AdminAiConfig.java)). VPN이 끊기거나 모델이 느리면 요청이 지연·실패하는데, 지금은 콘솔 스택트레이스 말고는 티가 안 난다. → 관측이 있으면 **그 라우트의 지연 급증·에러율**이 대시보드에 바로 뜨고, Loki에서 원인 로그로 점프할 수 있다.

2. **DB 커넥션 풀 고갈** — 즐겨찾기 카운트·상품 검색 등 조회가 무거워지거나 N+1이 생기면 HikariCP 풀이 마른다. → `hikaricp_connections_pending`(대기 중 요청) 지표로 **터지기 전에** 감지한다.

3. **힙 증가/OOM** — 시드 데이터, 리포트/상품 대량 조회 등에서 메모리가 새면, 지금은 앱이 죽고 나서야 안다. → `jvm_memory_used_bytes{area="heap"}` 추세로 **미리** 본다.

### 도입 후 지향점

- **감지**: JVM/HTTP/DB 지표를 대시보드로 상시 관찰, 임계치 초과 시 알림으로 즉시 인지.
- **추적**: 이상 시각·API를 지표로 좁힌 뒤, 같은 시간창의 Loki 로그로 넘어가 원인 특정.
- **공유**: Grafana URL 하나로 팀 전원이 같은 화면을 보며 상태 공유(구두 설명 불필요, 포트폴리오에서도 시연 가능).

---

## 3. 도구별 역할

| 도구 | 다루는 것 | 답하는 질문 | 한 줄 요약 |
|------|-----------|-------------|-----------|
| Prometheus | 메트릭(숫자 시계열): 요청수·지연·힙·DB 커넥션 | "지금/과거에 이 값이 얼마였나? 추세는?" | 지표를 주기적으로 긁어 저장하는 수집기 |
| Loki | 로그(텍스트 이벤트) | "그 시각에 무슨 일이 있었나? 어떤 에러가?" | 라벨 기반으로 로그를 모으는 경량 로그 저장소 |
| Grafana | 시각화·대시보드·알림 | "한눈에 어떤 상태인가? 이상하면 알려줘" | Prometheus·Loki를 붙여 보여주는 유일한 UI |

**협업 흐름 (실제 장애 대응)**
1. Grafana 대시보드에서 특정 API의 p95 지연이 튀거나 에러율이 오른 것을 **감지**(또는 알림 수신).
2. 해당 시각·라벨로 좁혀 Prometheus 지표(힙·DB 커넥션 등)로 **범위를 압축**.
3. 같은 시간창의 Loki 로그를 열어 예외 스택트레이스·요청을 확인해 **원인 특정** 후 조치.

---

## 4. 아키텍처 — 우리 스택 기준

### 메트릭 수집 (대상 → Prometheus)

| 대상 | 수집 방법 | 얻는 것 |
|------|-----------|---------|
| Spring Boot 앱 | Actuator + Micrometer가 `/actuator/prometheus` 노출 → Prometheus가 scrape | `http_server_requests`(요청수·지연), `jvm_memory_used_bytes`(힙), `hikaricp_connections`(DB 풀), `system_cpu_usage` |
| MySQL 8.0 | (후순위) `mysqld-exporter` 컨테이너 → Prometheus scrape | 커넥션 수, 쿼리 처리량, buffer pool |
| 호스트/컨테이너 | (후순위) `node-exporter`/`cAdvisor` | CPU·메모리·디스크 |

> 1차 목표는 **Spring 앱 메트릭**만. MySQL/시스템 exporter는 앱 지표 안정 후 선택 추가.

### 로그 수집 (앱 → Loki)

Loki는 로그를 스스로 읽지 않고, **수집 에이전트가 push**하는 구조다. **Grafana Alloy**(구 Promtail 후속)를 컨테이너로 띄워 Spring 앱 로그를 라벨(`app=dongnemarket`, `level` 등)과 함께 Loki로 전송한다. 앱이 호스트 `bootRun`으로 뜨는 동안에는 로그 파일(`logging.file.name`)을 Alloy가 tail 하는 방식으로 시작하고, 앱을 컨테이너화하면 도커 로그 드라이버로 전환한다.

### 전체 그림

```
                 scrape /actuator/prometheus
 ┌───────────────┐  (HTTP, 15s 주기)   ┌──────────────┐
 │  Spring App   │ ──────────────────► │  Prometheus  │─┐
 │ (bootRun)     │                     └──────────────┘ │  query
 │  :8080        │                                      ├──► ┌──────────┐
 │  log file ────┼── tail ──► ┌───────┐   push          │    │ Grafana  │
 └───────────────┘            │ Alloy │ ──────► ┌──────┐ │    │  :3000   │
                              └───────┘         │ Loki │─┘    └──────────┘
   MySQL :3306 (exporter는 후순위)              └──────┘      (대시보드·알림)
```

> Windows/Mac에서 앱을 호스트 `bootRun`으로 띄우면, 컨테이너 안의 Prometheus는 앱을 `localhost`가 아니라 **`host.docker.internal:8080`** 으로 불러야 한다(§8 참조).

---

## 5. 도입 로드맵

| 단계 | 내용 | 결과물 | 담당자 | 상태 |
|------|------|--------|--------|------|
| 0 | 문서화 + 도구 개념 학습 | 이 문서 | 팀장 | ✅ |
| 1 | 앱 메트릭 노출 (Micrometer-Prometheus) | `/actuator/prometheus` 응답 | 팀장 | ⬜ |
| 2 | Prometheus 기동 + 앱 scrape | Targets UP | 팀장 | ⬜ |
| 3 | Grafana 기동 + 첫 대시보드 | JVM/HTTP 대시보드 | 팀장 | ⬜ |
| 4 | Loki + Alloy 로그 수집 | Loki에 앱 로그 적재 | 팀장 | ⬜ |
| 5 | 메트릭↔로그 연계 | 지표에서 로그로 점프 | 팀장 | ⬜ |
| 6 | 알림(Alerting) 규칙 | 에러율/지연 알림 | 팀장 | ⬜ |

> 순서 원칙: **내보내는 쪽(앱) → 수집(Prometheus/Loki) → 보여주기(Grafana) → 알림** 순. 각 단계는 앞 단계의 헬스체크를 통과해야 다음으로 넘어간다. 한 번에 전부 띄우지 않는다.

---

## 6. 1단계 상세 — 앱 메트릭 노출

목표: Spring 앱이 Prometheus가 읽을 수 있는 포맷으로 지표를 노출하게 만든다. (스택을 세우기 전에 "내보낼 데이터"부터 확보)

### 6.1 파일 구조

```
backend/
├── build.gradle              # micrometer-registry-prometheus 의존성 추가
└── src/main/resources/
    └── application.yml        # management(actuator) 노출 설정 추가
```

> 이 단계는 앱 자체 변경만 있고 별도 컨테이너가 없다. 스택 컨테이너용 `monitoring/`은 2단계에서 만든다.

### 6.2 실행

```bash
docker compose up -d                       # (backend/) MySQL 먼저 — 앱 부팅 시 커넥션 풀이 접속
./gradlew bootRun                          # 앱 실행
curl http://localhost:8080/actuator/prometheus   # 메트릭 확인
```

접속:
- 메트릭 엔드포인트: http://localhost:8080/actuator/prometheus (인증 없이 노출 — **로컬 학습 한정**, 운영 반영 시 접근 제한 필요)

### 6.3 핵심 설정

`build.gradle` — actuator 아래에 레지스트리 추가:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
// Micrometer → Prometheus 포맷으로 메트릭 노출 (/actuator/prometheus)
implementation 'io.micrometer:micrometer-registry-prometheus'
```

`application.yml` — 엔드포인트 노출 + 공통 라벨:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus   # 딱 필요한 것만 공개
  metrics:
    tags:
      application: ${spring.application.name}   # 모든 지표에 app 라벨 → Grafana 구분 편함
```

> **주의**: 의존성(레지스트리) + 노출 설정(`exposure.include`에 `prometheus`)은 **한 쌍**이다. 하나만 하면 엔드포인트가 안 생기거나(의존성 누락) 막혀 있다(노출 누락). 이 설정은 인증 없이 지표를 공개하므로 로컬 전용이다.

### 6.4 헬스체크 — 통과 기준

1. `curl http://localhost:8080/actuator/prometheus`가 200 + 텍스트 지표 출력.
2. 응답에 `jvm_memory_used_bytes`, `http_server_requests_seconds_count`, `hikaricp_connections`가 존재.
3. Swagger 등으로 API 몇 번 호출 후 다시 조회 → `http_server_requests_seconds_count` 증가.
4. 모든 지표 라인에 `application="dongnemarket"` 라벨이 붙어 있음.

### 6.5 버전 (고정)

| 항목 | 버전 |
|------|------|
| micrometer-registry-prometheus | Spring Boot 3.5.15 BOM 관리(명시 버전 미지정) |
| Spring Boot Actuator | 3.5.15 (BOM) |

> 도커 이미지 태그 고정(Prometheus/Grafana/Loki/Alloy)은 2단계 이후 각 단계 문서에 기록(§10 컨벤션 1).

---

## 7. 돌렸을 때: 무엇을 어디서 보나

> 시스템이 켜진 상태에서 **일상 점검**과 **쿼리**를 어떻게 하는지. 아래 쿼리는 1단계(메트릭 노출)만 끝나면 Prometheus/Grafana에서 그대로 쓸 수 있다.

### 7.1 화면 지도

| 보고 싶은 것 | 어디서 | 어떻게 |
|--------------|--------|--------|
| 앱이 수집되고 있나 | Prometheus http://localhost:9090 | Status → Targets, `dongnemarket`이 **UP** |
| 요청량·지연·에러율 | Grafana 대시보드 | HTTP 패널 (아래 쿼리) |
| 힙/GC/스레드 | Grafana 대시보드 | JVM 패널 |
| DB 커넥션 풀 | Grafana 대시보드 | HikariCP 패널 |
| 그때 무슨 일이 | Grafana Explore(Loki) | LogQL (§7.3) |

### 7.2 자주 쓰는 PromQL (우리 라우트 기준)

```promql
# 초당 요청수 (라우트별)
sum by (uri) (rate(http_server_requests_seconds_count{application="dongnemarket"}[1m]))

# 5xx 에러율 (전체 대비)
sum(rate(http_server_requests_seconds_count{application="dongnemarket",status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count{application="dongnemarket"}[5m]))

# p95 지연 (라우트별) — 관리자 AI(/api/admin/ai/chat)가 특히 튈 수 있음
histogram_quantile(0.95,
  sum by (le, uri) (rate(http_server_requests_seconds_bucket{application="dongnemarket"}[5m])))

# 힙 사용량
sum(jvm_memory_used_bytes{application="dongnemarket",area="heap"})

# DB 커넥션: 사용중 / 대기 (pending이 0보다 크면 풀 부족 신호)
hikaricp_connections_active{application="dongnemarket"}
hikaricp_connections_pending{application="dongnemarket"}
```

### 7.3 자주 쓰는 LogQL (Loki, 4단계 이후)

```logql
{app="dongnemarket"} |= "ERROR"                          # 에러만
{app="dongnemarket"} |= "BusinessException"              # 우리 도메인 예외
{app="dongnemarket"} |~ "(?i)ollama|admin/ai"            # 관리자 AI 관련
{app="dongnemarket"} | json | level="ERROR"              # (JSON 로그로 전환 시)
```

### 7.4 일일 점검 루틴 (권장)

1. Prometheus Targets가 전부 UP인지 확인.
2. Grafana에서 최근 24h **5xx 에러율**과 **p95 지연**이 평소 대비 튀지 않았는지.
3. **힙 추세**가 우상향(누수 의심)인지.
4. `hikaricp_connections_pending`이 0 근처를 유지하는지.

---

## 8. 트러블슈팅

| 증상 | 원인 | 조치 |
|------|------|------|
| `bootRun`이 시작 중 죽음, `Communications link failure` | MySQL이 안 떠 있음 | `docker compose up -d`로 MySQL 먼저. `docker ps`로 `dongne-mysql` 확인 |
| `/actuator/prometheus` 404 | 레지스트리 의존성 또는 노출 설정 누락 | §6.3의 **두 가지**를 모두 적용했는지 확인 |
| Prometheus Targets가 **DOWN** | 컨테이너에서 `localhost:8080`을 부름 (앱은 호스트에 있음) | scrape 타깃을 `host.docker.internal:8080`으로 (Windows/Mac). Linux는 `--add-host=host.docker.internal:host-gateway` 또는 호스트 IP |
| Targets DOWN + 방화벽 | Windows 방화벽이 8080 인바운드 차단 | 로컬 8080 허용 또는 앱을 같은 도커 네트워크로 이동 |
| `docker compose up`가 포트 충돌 | 3000(Grafana)/9090(Prometheus) 이미 사용 중 | 점유 프로세스 종료 또는 compose에서 포트 매핑 변경 |
| Grafana가 Prometheus에 연결 못 함 | 데이터소스 URL이 `localhost` | 같은 compose 네트워크면 `http://prometheus:9090` 사용 |
| Grafana 로그인 안 됨 | admin 비밀번호 모름 | `.env`의 `GF_SECURITY_ADMIN_PASSWORD` 확인(§10 컨벤션 4) |
| 지표는 있는데 `application` 라벨 없음 | `metrics.tags.application` 미설정 | §6.3 `application.yml` 반영 |
| 관리자 AI 요청만 매우 느림/에러 | VPN/Ollama(`10.111.111.90`) 문제 | 앱 부팅은 정상(빈 조립만) — 첫 호출에서 실패. VPN·Ollama 상태 확인. 이것이 관측이 필요한 대표 케이스 |

---

## 9. 장애 대응 런북

> "느리다/에러난다" 신고를 받으면 위에서부터 따라간다. (스택 구축 완료 후 유효)

| 증상 | ① 먼저 볼 지표 | ② 좁히기 | ③ 로그(Loki) | 흔한 원인 |
|------|----------------|----------|--------------|-----------|
| 특정 API가 느림 | HTTP p95 지연(§7.2) | 어느 `uri`인지 특정 | 같은 시간창 `{app="dongnemarket"}` 해당 라우트 | 느린 쿼리, N+1, 외부의존(Ollama) |
| 에러율 급증 | 5xx 에러율(§7.2) | status·uri별 분해 | `|= "ERROR"`, `|= "Exception"` | 배포 회귀, 잘못된 입력, DB 이슈 |
| 앱이 죽음/힙 증가 | `jvm_memory_used_bytes` 추세 | GC 빈도·live set | OOM 직전 로그 | 메모리 누수, 대량 조회 |
| 응답 지연 + DB 대기 | `hikaricp_connections_pending` | active vs max | 슬로우 쿼리 로그 | 풀 고갈, 트랜잭션 장기 점유 |
| 관리자 AI 무응답 | `/api/admin/ai/chat` p95·에러 | 그 라우트만 격리 | `|~ "ollama|admin/ai"` | VPN 끊김, 모델 지연 |

각 행의 흐름: **지표로 감지 → 라벨로 범위 압축 → 로그로 원인 확정.** 조치 후에는 같은 지표가 정상으로 돌아오는지 대시보드로 재확인한다.

---

## 10. 팀 컨벤션 (반드시 지킬 것)

1. **이미지 태그를 고정한다.** `latest` 금지 — `prom/prometheus:v3.x.x`처럼 명시 태그를 쓰고 각 단계 §버전에 기록(재현성).
2. **모니터링 스택은 별도 compose로 분리한다.** MySQL용 `backend/docker-compose.yml`과 섞지 말고 `monitoring/docker-compose.yml`로 둔다(앱과 독립적으로 on/off).
3. **설정은 코드로 관리한다(as-code).** Grafana 데이터소스·대시보드, Prometheus 타깃, Alloy 파이프라인은 UI 수동 설정 대신 프로비저닝/설정 파일로 리포에 커밋한다.
4. **비밀번호·토큰을 커밋하지 않는다.** Grafana admin 비밀번호 등은 `.env`(gitignore)로 주입한다.
5. **각 단계는 헬스체크 통과 후 다음으로.** 한 번에 전부 띄워 디버깅 지옥에 빠지지 않는다.
6. **대시보드를 바꾸면 리포에도 반영한다.** Grafana UI에서만 고치고 끝내지 말고 export 해서 커밋(다음 사람이 재현 가능하게).

---

## 11. 용어 정리

| 용어 | 뜻 |
|------|-----|
| 메트릭(Metric) | 시간에 따라 변하는 숫자값(요청 수, 힙 사용량 등). 시계열로 저장·집계. |
| scrape | Prometheus가 대상의 엔드포인트를 주기적으로 HTTP로 긁어와 수집하는 것. |
| exporter | 프로메테우스 포맷을 직접 못 내는 대상(MySQL 등)을 대신 노출해주는 어댑터. |
| Micrometer | 앱 코드가 특정 백엔드에 종속되지 않게 지표를 측정하는 추상화("메트릭의 SLF4J"). |
| Actuator | Spring Boot가 앱 상태·지표를 `/actuator/**`로 노출하는 표준 모듈. |
| 라벨(Label) | 지표/로그에 붙는 key=value 태그(`application="dongnemarket"`). 필터·그룹 기준. |
| PromQL / LogQL | Prometheus 지표 / Loki 로그를 질의하는 언어. |
| histogram / p95 | 응답시간 분포. p95 = 95백분위("느린 편 요청"의 체감 지표). |
| Loki | 라벨 기반 경량 로그 저장소(PromQL의 로그판). |
| Alloy | 로그·지표를 수집해 Loki/Prometheus로 보내는 Grafana 수집 에이전트(구 Promtail 후속). |
| host.docker.internal | 컨테이너 안에서 **호스트 머신**을 가리키는 도커 특수 호스트명(Win/Mac). |
| 프로비저닝(provisioning) | Grafana 등을 UI 클릭 대신 설정 파일로 자동 구성하는 것. |

---

## 12. 진행 체크리스트

- [x] 0단계: 이 문서 작성 및 팀 공유
- [ ] 1단계: `/actuator/prometheus`에서 앱 지표 노출 확인(§6.4 통과)
- [ ] 2단계: Prometheus 기동 후 앱 타깃 UP 확인
- [ ] 3단계: Grafana에서 JVM/HTTP 첫 대시보드 확인
- [ ] 4단계: Loki에 앱 로그가 라벨과 함께 적재됨
- [ ] 5단계: Grafana에서 로그 조회 + 지표→로그 점프
- [ ] 6단계: 에러율/지연 알림 규칙 동작 확인
