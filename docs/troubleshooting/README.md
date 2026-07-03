# 트러블슈팅 — 로컬 실행 / DB 기동

> 대상: 동네마켓 팀 전원 · 목적: 로컬에서 앱이 안 뜰 때 **증상으로 검색**해 바로 원인·조치를 찾는 레퍼런스.
> 각 항목은 **증상 → 로그 시그니처 → 원인 → 조치 → 예방** 순서. 새 사고를 겪으면 같은 형식으로 아래에 추가한다.

관련 문서: 관측 스택 트러블슈팅은 [observability §8](../observability/README.md#8-트러블슈팅) 참고.

---

## 0. 먼저 — 로컬 실행 순서 (사고 예방의 90%)

이 프로젝트 로컬 실행은 **항상 이 순서**다. 순서를 어기면 아래 사고들이 난다.

```
① Docker Desktop 실행 (데몬 준비까지 대기)
      ↓
② MySQL 기동 + healthy 대기
   cd backend && docker compose up -d --wait
      ↓
③ 앱 실행 (한 곳에서만! bootRun 또는 IntelliJ 중 하나)
   ./gradlew bootRun
```

> 앱은 부팅 중 Hibernate가 스키마를 확인하려고 **시작 시점에 DB에 붙는다**(`ddl-auto: update`). 그래서 DB가 준비 안 되면 fail-fast로 죽는다.

---

## 1. 앱이 부팅 중 죽음 — MySQL에 연결 실패

**증상**: `./gradlew bootRun`이 시작하다가 `entityManagerFactory` 빈 생성에서 실패하며 종료(exit code 1).

**로그 시그니처**:
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
...
Caused by: java.net.ConnectException: Connection refused: getsockopt
```

**원인**: 앱이 접속하려는 MySQL(`localhost:3306`)이 없다. 대개 둘 중 하나:
- MySQL 컨테이너가 안 떠 있음
- Docker Desktop 자체가 꺼져 있음 (→ 2번 항목)

**조치**:
```bash
docker ps                     # dongne-mysql 이 목록에 있는지
# 없으면:
cd backend
docker compose up -d --wait   # healthy 될 때까지 대기 후 반환
docker ps                     # STATUS가 (healthy) 인지 확인
./gradlew bootRun             # 다시 실행
```

**예방**: `--wait` 플래그 + healthcheck(§4). MySQL이 `healthy`가 된 뒤에 앱을 띄운다.

---

## 2. Docker 명령이 전부 실패 — Docker Desktop 다운

**증상**: `docker ps` / `docker compose`가 에러. 3306·9090 등 모든 컨테이너 포트가 비어 있음.

**로그 시그니처**:
```
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine;
check if the path is correct and if the daemon is running
```
또는 기동 중일 때:
```
request returned 500 Internal Server Error for API route ...
```

**원인**: Docker Desktop(데몬)이 꺼져 있거나 아직 초기화 중. Windows에서 재부팅·절전 후 자주 발생.

**조치**:
1. **Docker Desktop 실행** (시작 메뉴 또는):
   ```bash
   "/c/Program Files/Docker/Docker/Docker Desktop.exe" &
   ```
2. **데몬 준비까지 대기** — 초기화 중엔 `docker info`가 500을 반환하거나 응답이 지연된다. 준비될 때까지 폴링:
   ```bash
   until docker info >/dev/null 2>&1; do sleep 5; done; echo "Docker READY"
   ```
3. 준비되면 §1의 순서로 MySQL부터 기동.

**예방**: 작업 시작 시 Docker Desktop이 떠 있는지 먼저 확인(트레이 아이콘). 준비에 30~90초 걸릴 수 있으니 서두르지 말 것.

---

## 3. 앱이 뜨다가 죽음 — Port 8080 already in use

**증상**: DB 연결은 성공(`HikariPool-1 - Added connection`)했는데, 마지막에 Tomcat 바인딩에서 실패.

**로그 시그니처**:
```
APPLICATION FAILED TO START
Description:
Web server failed to start. Port 8080 was already in use.
```

**원인**: **앱을 두 번 띄웠다.** 거의 항상 **IntelliJ Run + 터미널 `bootRun`을 동시에** 실행한 경우. 둘 다 8080을 원해 뒤에 뜬 쪽이 죽는다.

**조치** (Windows / Git Bash):
```bash
# 1) 8080을 잡고 있는 PID 찾기 (맨 오른쪽 숫자)
netstat -ano | findstr :8080 | findstr LISTENING

# 2) 그 PID 종료 (예: 10152)
taskkill //F //PID 10152
#   ※ Git Bash에서는 슬래시 두 개(//F //PID)로 써야 경로로 오인 안 됨

# 3) 한 곳에서만 다시 실행
./gradlew bootRun
```
또는 IntelliJ에서 실행 중이면 **빨간 ■ Stop**으로 끄고 하나만 사용.

**주의**: `netstat`에 8080이 안 보이는데도 "in use"가 나면 → 직전 인스턴스가 종료 중(포트 해제 지연)이거나 Docker 컨테이너가 8080을 매핑 중일 수 있다. `docker ps`로 8080 매핑 컨테이너를 확인.

**예방**: **앱은 항상 한 경로로만 실행.** 로컬 개발은 터미널 `bootRun` 하나로 통일하면 로그도 보기 편하다.

---

## 4. 근본 처방 — healthcheck (컨테이너 `Up` ≠ DB `ready`)

위 1번 사고의 핵심 원인은 **타이밍**이다. `docker compose up -d`로 MySQL 컨테이너가 `Up` 상태가 돼도, 그 안의 `mysqld`가 **접속을 받을 준비가 되기까지 몇 초의 공백**이 있다. 이 공백에 앱의 HikariCP가 fail-fast로 즉시 접속을 시도하면 `Connection refused`로 죽는다.

**즉 "컨테이너가 떴다(Up)" ≠ "DB가 준비됐다(ready)".** 이 둘을 구분해주는 게 healthcheck다.

`backend/docker-compose.yml`의 MySQL 서비스에 추가한 설정:
```yaml
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p${MYSQL_ROOT_PASSWORD:-root1234}"]
      interval: 10s      # 10초마다 검사
      timeout: 5s        # 5초 내 응답 없으면 실패로 간주
      retries: 10        # 10번 연속 실패해야 unhealthy
```

`mysqladmin ping`이 성공해야 컨테이너 상태가 `(healthy)`로 바뀐다. 이걸 두 곳에서 활용한다:

- **dev 모드**: `docker compose up -d --wait` → MySQL이 `healthy`가 될 때까지 **명령이 반환을 미룬다**. 반환된 뒤 `bootRun` 하면 공백이 없다.
- **full(도커) 모드**: 앱 서비스가 `depends_on: { mysql: { condition: service_healthy } }` → 컴포즈가 **MySQL healthy 이후에 앱 컨테이너를 시작**한다(순서 자동 보장).

**확인**:
```bash
docker compose up -d --wait
docker ps        # dongne-mysql 의 STATUS 열에 (healthy) 표시
```

> 참고: prod 컴포즈(`docker-compose.prod.yml`)는 원래부터 healthcheck + `depends_on: service_healthy`가 있었다. dev에는 없어서 비대칭이었고, 이번에 dev에도 넣어 맞췄다. 배경: [실행/운영 환경 2모드 정리](../deployment/README.md).

---

## 부록 — 빠른 진단 명령 모음

```bash
# 컨테이너 상태 + 포트 매핑 한눈에
docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Status}}"

# 특정 포트를 누가 잡고 있나 (Windows)
netstat -ano | findstr :3306      # MySQL
netstat -ano | findstr :8080      # App
netstat -ano | findstr :9090      # Prometheus

# 실행 중인 java 프로세스 (중복 실행 점검)
tasklist | findstr -i java

# 앱이 실제로 메트릭을 내보내는지
curl http://localhost:8080/actuator/prometheus | head
```
