# 인프라 인벤토리 (as-is)

> 최종 수정일: 2026-07-08 · 상태: draft · 기준: `refactor/infra-boundary` (#198 + P2 프로파일 + P3 지형 + P4 Flyway 반영)
> **as-is 스냅샷.** 목표 구조와 재정리 결정은 [ADR 0004](../adr/0004-infra-boundary.md) 참고. 리팩토링(P2~P4) 진행에 따라 이 문서를 갱신한다.

"환경·인프라 세팅이 지금 어떻게 되어 있는지"를 **누가 무엇을 담당하는지(경계)** 관점으로 정리한 목록. 배포 토폴로지의 물리 구성은 [05-deployment.md](05-deployment.md), 로컬 컨테이너 구성은 [02-container.md](02-container.md)를 함께 본다.

## 1. 최상위 폴더 경계 — 무엇이 어디에 있나

| 폴더/파일 | 담당 영역 | 대상 지형 |
| --- | --- | --- |
| [backend/](../../backend), [frontend/](../../frontend) | 앱 소스 + **Dockerfile**(이미지 정의) | 전 지형 공용 |
| [docker-compose.yml](../../docker-compose.yml) (루트) | **dev** 실행 — MySQL만(앱·프론트는 호스트) | 개발 |
| [infra/onprem/](../../infra/onprem) | **온프레미스** 전체 스택 compose + `nginx.conf` + `monitoring/` | 온프레미스 전용 |
| [infra/cloud/](../../infra/cloud) | **클라우드** 배포 자원(EC2별 폴더) | 클라우드 전용 |
| [.github/workflows/](../../.github/workflows) | CI(테스트 게이트) + CD(빌드·push·배포) | 클라우드 파이프라인 |
| [.env.example](../../.env.example) + 각 하위 `.env.example` | 필요한 시크릿/변수 키 목록 | 지형별 |

## 2. Application 설정 (Spring 프로파일) — `backend/src/main/resources/`

| 파일 | 프로파일 | ddl-auto | 특징 | 쓰이는 곳 |
| --- | --- | --- | --- | --- |
| [application.yml](../../backend/src/main/resources/application.yml) | (공통 base) | — | DB·JWT·Mail·Ollama·**file.storage**·multipart, `default: dev` | 전부 |
| [application-dev.yml](../../backend/src/main/resources/application-dev.yml) | `dev` | `update` | 호스트 bootRun, SQL 로그 ON, prometheus 노출 | **개발** |
| [application-prod.yml](../../backend/src/main/resources/application-prod.yml) | `prod` | **`validate`** | Flyway가 스키마 담당·Hibernate는 검증만. prometheus 노출, quiet 로그 | **온프레미스 + 클라우드** |
| [application-demo.yml](../../backend/src/main/resources/application-demo.yml) | `demo` | `create` | 더미 시딩, drop+create ⚠️ | 개발 add-on (`dev,demo`) |

> `prod`는 Flyway가 [`db/migration/V*.sql`](../../backend/src/main/resources/db/migration)로 스키마를 만들고 `ddl-auto: validate`로 검증만 한다(P4 완료, [ADR 0005](../adr/0005-flyway-migration.md)). dev/test는 Flyway OFF + ddl-auto 유지.

## 3. 파일 저장소 — 설정 기반(local/S3), #198 이후

프로파일이 아니라 `file.storage.type` **설정값**으로 구현체가 갈린다(`global/storage` 모듈).

| `FILE_STORAGE_TYPE` | 구현체 | 저장 위치 | 자격증명 | 쓰는 지형 |
| --- | --- | --- | --- | --- |
| `local` (기본값) | `LocalFileStorageService` | `FILE_STORAGE_LOCAL_BASE_PATH`(기본 `./uploads`) | — | 개발 · 온프레미스 |
| `s3` | `S3FileStorageService` | `AWS_S3_BUCKET`/`AWS_REGION` | AWS SDK 기본 체인(EC2 IAM Role) | 클라우드 |

- 온프레미스(`infra/onprem`): `local` + `dongne-uploads` 볼륨(신고 증빙·상품 이미지 공용).
- 클라우드(infra/cloud/app): `FILE_STORAGE_TYPE=s3` 고정, 볼륨 없음.

## 4. Dockerfile — 이미지 정의 (지형 무관 공용)

| 파일 | 방식 | 산출물 |
| --- | --- | --- |
| [backend/Dockerfile](../../backend/Dockerfile) | JDK21 빌드 → JRE 런타임 2-stage, `bootJar -x test` | `:8080` 경량 JAR 이미지 |
| [frontend/Dockerfile](../../frontend/Dockerfile) | node22 deps→build→standalone 3-stage | `:3000` Next SSR standalone |

## 5. docker-compose — 실행 단위

| 파일 | 지형 | 담는 컨테이너 | 이미지 출처 | `SPRING_PROFILES_ACTIVE` |
| --- | --- | --- | --- | --- |
| [docker-compose.yml](../../docker-compose.yml) (루트) | **dev** | mysql만 | — | — (호스트 bootRun = `dev`) |
| [infra/onprem/](../../infra/onprem/docker-compose.yml) | **온프레미스** | mysql·app·next·nginx / (observability)prom·grafana·loki·promtail / (edge)cloudflared | `build:` 로컬 빌드 | `prod` |
| [infra/cloud/app/](../../infra/cloud/app/docker-compose.yml) | 클라우드 앱 EC2 | nginx·next·app·promtail | **ECR pull** | `prod` |
| [infra/cloud/db/](../../infra/cloud/db/docker-compose.yml) | 클라우드 DB EC2 | mysql(자체 호스팅) | Docker Hub | — |
| [infra/cloud/monitoring/](../../infra/cloud/monitoring/docker-compose.yml) | 클라우드 모니터링 EC2 | prometheus·loki·grafana | Docker Hub | — |

onprem compose 프로파일: (기본) 전체 스택 / `observability`(관측) / `edge`(cloudflared 퀵터널).

## 6. infra/cloud/ 내부 — EC2별 자원

| 폴더 | 올라가는 EC2 | 설정 파일 |
| --- | --- | --- |
| `app/` | 앱 EC2 | docker-compose.yml, [nginx.conf](../../infra/cloud/app/nginx.conf), [promtail-config.yml](../../infra/cloud/app/promtail-config.yml), .env.example |
| `db/` | DB EC2 | docker-compose.yml, .env.example |
| `monitoring/` | 모니터링 EC2 | docker-compose.yml, [prometheus.yml](../../infra/cloud/monitoring/prometheus.yml), loki-config.yml, grafana/provisioning/, .env.example |

## 7. CI/CD — `.github/workflows/`

| 파일 | 트리거 | 하는 일 | 경계 |
| --- | --- | --- | --- |
| [ci.yml](../../.github/workflows/ci.yml) | feature push · develop PR | 백엔드 test(H2) + 프론트 lint·build | **테스트 게이트만**, 배포 안 함 |
| [cd-app.yml](../../.github/workflows/cd-app.yml) | develop push(backend/frontend 변경) | 바뀐 것만 빌드→ECR push(OIDC)→앱 EC2 SSH pull·up | **배포 전담** |

이미지는 `:latest`와 `:{sha}` 두 태그로 push되지만, **배포는 `:latest`만 사용**(sha 태그 미활용 → 특정 커밋 롤백 수동).

## 8. ⚠️ 경계가 흐린 곳 (개선 후보 → [ADR 0004](../adr/0004-infra-boundary.md))

| # | 흐린 경계 | 현재 상태 | 해소 단계 |
| --- | --- | --- | --- |
| A | ~~`local` 프로파일 이중 역할~~ | ✅ **P2 완료(2026-07-08)** — `application-prod.yml` 신설·`local` 폐기, compose `SPRING_PROFILES_ACTIVE=prod` 전환 | ✅ 완료 |
| B | ~~운영 `ddl-auto: update`~~ | ✅ **P4 완료(2026-07-08)** — Flyway 도입·V1 baseline·prod `validate` 전환([ADR 0005](../adr/0005-flyway-migration.md)) | ✅ 완료 |
| C | **nginx.conf 중복** | [onprem](../../infra/onprem/nginx.conf)·[cloud](../../infra/cloud/app/nginx.conf) 거의 동일 복붙 | P4(잔여) |
| D | **monitoring 설정 이중화** | [onprem/monitoring/](../../infra/onprem/monitoring)·[cloud/monitoring/](../../infra/cloud/monitoring) 병존 (단 prometheus 타깃은 정당한 차이) | P4 |
| E | **CD `:latest` 배포** | sha 태그는 push만 하고 미사용 → 롤백 수동 | P4 |

> ✅ **P3 완료(2026-07-08)**: 온프레미스 배포를 [`infra/onprem/`](../../infra/onprem/)로 분리(compose·nginx·monitoring 이관), 루트 compose는 dev 전용으로 축소 — 배포 지형 경계 확립.
