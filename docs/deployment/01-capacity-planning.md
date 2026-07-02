# 01. 로컬 실측 벤치마크 & AWS 인스턴스 선정

> 목적: 감이 아니라 **실측 수치**로 AWS 컴퓨팅을 비용까지 고려해 선정한다.
> 측정일: 2026-07-01

---

## 1. 측정 환경

| 항목 | 값 |
|------|-----|
| 호스트 | Windows 11, 물리 RAM **7.35 GB** (⚠️ 타깃 EC2와 다름 → 시뮬레이션 필요) |
| JDK | Java 21 |
| 앱 | Spring Boot 3.5.15 실행 JAR (`bootJar`) |
| DB | MySQL 8.0 컨테이너 (`dongne-mysql`, docker-compose) |
| JVM 옵션 | `-Xms128m -Xmx384m` — **1GB 마이크로 인스턴스를 흉내내기 위해 힙 상한 제약** |

> ⚠️ **왜 힙을 제약했나**: JVM 기본 최대 힙 = 물리 RAM의 25%. 이 호스트에선 약 1.8GB가 잡혀
> 그대로 재면 타깃(1~2GB EC2)과 무관한 수치가 나온다. 그래서 마이크로 상황과 유사하게 384m로 고정했다.

---

## 2. 실측 결과

### 2-1. Spring Boot (JVM, `-Xmx384m`)

| 지표 | IDLE(기동 안정화) | 부하 후(300 req, 동시 10) |
|------|------------------|--------------------------|
| RSS (WorkingSet) | **300 MB** | **342 MB** |
| Private (Commit) | 366 MB | 404 MB |
| 스레드 수 | 62 | 62 |
| 기동 시간 | **7.6 초** | - |
| 처리량 | - | ~61 req/s (300req / 4.9s) |

### 2-2. MySQL 8 컨테이너

| 지표 | IDLE | 앱 부하 동반 시 |
|------|------|----------------|
| MEM USAGE | **393 MB** | **405 MB** |
| CPU | ~1% | ~1.2% |

### 2-3. 합산 추정 (Linux 타깃 기준)

앱 + DB + OS(리눅스 커널/서비스 ~150~250MB)를 합치면:

| 상태 | 앱 | MySQL | OS | **합계** |
|------|----|-------|----|---------|
| IDLE | 300 | 393 | ~200 | **≈ 893 MB** |
| 부하 | 342 | 405 | ~200 | **≈ 947 MB** |

> **핵심 결론**: app + MySQL 동거 시 상시 사용량이 **약 0.9GB**.
> 1GB 인스턴스는 **여유가 사실상 0** → 스왑 없이는 OOM Killer 위험이 매우 높다.

### 측정 재현 방법 (참고)

```powershell
# 1) MySQL 이미 기동된 상태에서 JAR 빌드
./gradlew bootJar -x test

# 2) 마이크로 시뮬레이션으로 실행
java -Xms128m -Xmx384m -jar build/libs/dongnemarket-0.0.1-SNAPSHOT.jar

# 3) 메모리 측정
Get-Process java | Select WorkingSet64, PrivateMemorySize64
docker stats --no-stream dongne-mysql
```

---

## 3. 측정 신뢰도 / 한계 (정직하게)

- **OS 차이**: 측정은 Windows WorkingSet 기준. 리눅스 RSS는 JVM에서 대체로 약간 더 낮게 나오지만 자릿수는 동일하다. 합산 추정은 보수적으로 잡았다.
- **JVM 비힙 메모리**: RSS에는 힙 외 메타스페이스(~40~60MB), 스레드 스택(62×수백KB), 다이렉트 버퍼가 포함된다. 힙 상한을 올리면 RSS도 함께 커진다.
- **데이터 볼륨**: 현재 시드 수준. 실제 상품/이미지 메타데이터가 쌓이면 MySQL 버퍼풀·앱 캐시가 더 커질 수 있다.
- **부하 현실성**: 로컬 루프백 300요청은 스모크 수준. 실제 동시 사용자 부하 테스트(k6/nGrinder)는 배포 후 별도 수행 권장.

---

## 4. 로컬 실행 중 도출된 문제점 (배포 전 조치 필요)

| # | 문제 | 위험 | 조치 방향 |
|---|------|------|----------|
| P1 | `ddl-auto: update` | 운영 스키마 자동 변경 → 데이터 사고 | prod 프로파일에서 `validate` (또는 Flyway 도입) |
| P2 | JWT 시크릿 · DB 비밀번호 **기본값 하드코딩** | 소스 유출 = 인증 우회 | 환경변수/`.env`(서버 로컬) 또는 SSM 파라미터스토어 주입 |
| P3 | `logging.level org.hibernate.SQL: debug` + `format_sql` | 운영 로그 폭증·성능 저하 | prod에선 `info` 이상, SQL 로그 off |
| P4 | 마이크로 인스턴스(1GB)에서 Gradle 빌드 | 빌드 중 OOM | **서버 빌드 금지** → 로컬/CI 빌드 후 아티팩트 전송 (수동배포 방침과 일치) |
| P5 | 헬스체크 엔드포인트 없음 (actuator 미포함) | 배포 검증·모니터링 곤란 | `spring-boot-starter-actuator` 추가, `/actuator/health` 노출 |
| P6 | app+DB 동거 시 1GB 여유 ≈ 0 | 트래픽 스파이크 시 OOM | **스왑 2GB 필수** + 힙/버퍼풀 튜닝 (아래 6장) |
| ✔ | `open-in-view: false` | (이미 적용됨 — 좋음) | 유지 |

---

## 5. AWS 인스턴스 후보 비교 (ap-northeast-2 서울)

> 가격은 온디맨드 Linux **대략치(월 730h 기준)**이며 변동된다. **반드시 AWS 요금 계산기로 재확인**할 것.

| 타입 | vCPU | RAM | 프리티어 | 대략 월 요금* | app+MySQL 동거 적합성 |
|------|------|-----|---------|--------------|----------------------|
| **t2.micro** | 1 | 1 GB | ✅ (12개월, 서울 프리티어 대상) | ~$10.5 (프리티어면 $0) | △ 스왑+튜닝 필수, 여유 없음 |
| t3.micro | 2 | 1 GB | 리전따라 | ~$9.5 | △ 1GB 동일 제약, vCPU 2라 t2보다 나음 |
| t4g.micro (ARM) | 2 | 1 GB | 리전따라 | ~$7.6 | △ 최저가지만 arm64 이미지 필요 |
| **t3.small** | 2 | 2 GB | ❌ | ~$19 | ◎ 편안 |
| **t4g.small (ARM)** | 2 | 2 GB | ❌ | ~$15 | ◎ 편안 + 최저가(단 arm64) |

*EBS(gp3 30GB 프리티어 무료), 아웃바운드 트래픽(월 100GB 무료), 탄력적 IP는 별도.

> ⚠️ **프리티어 정책 변경 주의**: AWS는 2025-07 이후 **신규 계정에 "12개월 무료" 대신 크레딧($100~) 방식**을 적용한다.
> 계정 생성 시점에 따라 무료 개념이 다르니 [AWS Free Tier 콘솔](https://console.aws.amazon.com/billing/home#/freetier)에서 본인 계정 유형을 먼저 확인할 것.

---

## 6. 선정 권고

### (A) 무료가 필수라면 — **t2.micro (프리티어)**
동작은 시키되 "빡빡함"을 인정하고 튜닝으로 버틴다.

- **스왑 2GB 필수**
  ```bash
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
  sudo mkswap /swapfile && sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  ```
- **JVM 힙 상한**: `-Xmx320m ~ -Xmx384m` (실측상 부하 후 RSS 342MB → 384m가 상한 현실선)
- **MySQL 버퍼풀 축소**: `innodb_buffer_pool_size=128M` (compose command에 추가)
- 리스크: 트래픽/데이터 증가 시 스왑 스래싱 → 응답 지연. **테스트 서버 용도로만** 권장.

### (B) 소액 지출이 가능하다면 — **t4g.small(2GB, ARM) 또는 t3.small(2GB)** (권장)
실측 합산 0.9GB에 **2배 여유(2GB)** → 페이지 캐시·스파이크·부하테스트까지 안정.
- ARM(t4g)이 x86(t3)보다 저렴(~$15 vs ~$19). 단 도커 이미지를 **arm64(linux/arm64)** 로 빌드해야 함.
- 실무형/시연용이면 이 등급을 강력 권장.

### 종합 판단
> **테스트 서버 = t2.micro(프리티어) + 스왑2G + 힙384m + 버퍼풀128M 로 시작**,
> 안정성/시연이 중요해지면 **t4g.small(2GB)로 승격**. 코드/배포 구성은 두 경우 모두 동일하게 유지 가능.

---

## 7. 다음 단계 (별도 작업)

1. `application-prod.yml` 분리 — P1~P3 반영 (`validate`, SQL 로그 off, 시크릿 환경변수)
2. actuator 추가 — 헬스체크 (P5)
3. `Dockerfile` (JRE 21 slim, 레이어 캐시) + `docker-compose.prod.yml` (app + MySQL + 버퍼풀 튜닝)
4. EC2 셋업 런북 — 스왑, 보안그룹(8080/22), 아티팩트 전송(scp/`docker save`) 절차
