# 0004. 인프라를 2축(운영 수위 × 배포 지형)으로 경계 재정리

> 상태: Accepted · 날짜: 2026-07-08

## 배경 (Context)

인프라·환경 세팅이 초기에 명확한 경계 없이 쌓여, 팀원이 "어디를 고쳐야 어느 환경이 바뀌는지" 파악하기 어렵다. 특히 **개발 / 운영 / 온프레미스 배포 / 클라우드 배포** 라는 4개 낱말이 뒤섞여 쓰이는데, 실측해 보면 이들은 평행한 4개 환경이 아니라 **서로 다른 두 개의 축**이다.

현재 상태(실측, `dc94fa9` 기준)의 경계 문제:

- **`local` 프로파일이 이중 역할** — 온프레미스 로컬 배포와 클라우드 배포가 [application-local.yml](../../backend/src/main/resources/application-local.yml) 하나를 공유한다. 두 배포의 차이는 프로파일이 아니라 compose 파일 + env 변수로만 갈린다.
- **클라우드 전용 프로파일 부재** — 운영에서도 `ddl-auto: update`가 돈다([ADR 0003](0003-schema-ddl-auto.md)).
- **설정 중복** — `nginx.conf`가 [로컬용](../../nginx/nginx.conf)과 [클라우드용](../../infra/cloud/app/nginx.conf) 두 벌로 복붙돼 있어 드리프트 위험.
- 전체 as-is 목록은 [07-infra-inventory.md](../architecture/07-infra-inventory.md) 참고.

한편 [#198](https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/198)이 저장소 선택을 프로파일이 아닌 `file.storage.type`(local/s3) **설정값**으로 분리하면서, 이 방향(수위와 지형을 나누는)의 첫 조각은 이미 들어왔다.

## 결정 (Decision)

인프라를 **두 개의 독립된 축**으로 나눠 관리한다.

- **축① 운영 수위** (앱이 *어떻게* 동작하나) → **Spring 프로파일**: `dev` / `prod`
- **축② 배포 지형** (컨테이너가 *어디서* 도나) → **compose 위치**: 루트(개발) / `infra/onprem/`(온프레미스) / `infra/cloud/`(클라우드)

"운영"은 장소가 아니라 수위다. **온프레미스 배포와 클라우드 배포는 둘 다 `prod` 수위**로 돌고, 둘의 차이는 프로파일이 아니라 **env 바인딩**(`FILE_STORAGE_TYPE`·`DB_URL`·이미지 출처)으로만 가른다. 지금의 `local` 프로파일(이중 역할)은 `prod`로 흡수·폐기한다.

## 결과 (Consequences)

**좋은 점**
- 팀원이 "프로파일 = 동작 수위, 폴더 = 배포 지형"이라는 두 축만 알면 전체 구조를 이해할 수 있다.
- 온프레미스·클라우드가 같은 `prod` 프로파일/같은 앱 이미지/같은 nginx 라우팅을 공유해 환경 간 동작 동일성이 유지된다.
- 클라우드 프로파일이 생기면 [ADR 0003](0003-schema-ddl-auto.md)의 "운영 `ddl-auto: update`" 위험을 해소할 자리가 마련된다.

**감수하는 비용 / ⚠️ 위험**
- 프로파일 이름 변경(`local`→`prod`)은 모든 compose의 `SPRING_PROFILES_ACTIVE`와 배포 스크립트를 동시에 바꿔야 한다 — 누락 시 기동 실패.
- `ddl-auto: validate` 전환은 스키마 베이스라인(Flyway 등)이 없으면 클라우드 기동을 깨뜨린다. **이름 경계(P2)와 스키마 전환(P4)을 분리**해 순서대로 진행한다.

**후속 (단계 로드맵)**

| 단계 | 내용 | 산출물 |
| --- | --- | --- |
| **P1** (본 ADR) | as-is 인벤토리 + 2축 설계 문서화 | 이 ADR, [07-infra-inventory.md](../architecture/07-infra-inventory.md) |
| **P2** | `application-prod.yml` 신설, `local` 폐기, compose들 `SPRING_PROFILES_ACTIVE=prod`로 전환 (단 `ddl-auto`는 아직 `update` 유지) | 프로파일 경계 확립 |
| **P3** | `infra/onprem/` 신설·이관, 루트 `docker-compose.yml`을 dev(개발) 전용으로 축소 | 지형 경계 확립 |
| **P4** | `nginx.conf` 단일화, CD를 `:sha` 태그 배포로, **Flyway 도입 후 클라우드 `ddl-auto: validate` 전환**(0003 해소용 새 ADR) | 중복 제거 · 운영 안전화 |

## 대안 (Alternatives)

- **환경마다 프로파일 1개씩(dev/onprem/cloud/prod 4개)**: 직관적으로 보이나, 온프레미스·클라우드가 사실상 같은 운영 수위라 프로파일이 갈라지면 두 파일이 계속 드리프트한다. 2축으로 나누면 프로파일은 2개(dev/prod)로 충분.
- **현행 유지(`local` 이중 역할)**: 세팅 비용은 0이지만, 온보딩 난이도와 운영 `ddl-auto` 위험이 그대로 남는다 → 팀 확장 시점에 재발.
