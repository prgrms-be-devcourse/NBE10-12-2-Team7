# 배포(Deployment) 문서

동네마켓 백엔드를 AWS(프리티어 우선)에 배포하기 위한 실무형 준비 문서 모음.
"단순 도입"이 아니라 **로컬 실측 → 문제점 도출 → 비용 고려한 인스턴스 선정 → 문서화 → 본격 도입** 순서로 진행한다.

## 문서 목록

| 번호 | 문서 | 내용 |
|------|------|------|
| 01 | [01-capacity-planning.md](01-capacity-planning.md) | 로컬 실측 벤치마크 + 문제점 + AWS 인스턴스 선정/비용 비교 |
| 02 | [02-build-fix.md](02-build-fix.md) | 빌드 가능화 — Report 리팩터링 통합 갭으로 깨진 컴파일 복구 |
| 03 | [03-local-deploy.md](03-local-deploy.md) | 로컬 배포용(전부 Docker: web·observability·edge) 실행 런북 — 팀원 온보딩용 |
| 04 | [04-local-deploy-build.md](04-local-deploy-build.md) | 로컬 배포 환경 **구축 과정·설계 이유·유지보수 변경 포인트** |
| 05 | [05-aws-cicd-design.md](05-aws-cicd-design.md) | **AWS 배포 & CI/CD 전체 설계 방향** — EC2×2 + RDS + ECR, GitHub Actions 파이프라인 |
| 06 | [06-ec2-app-deploy.md](06-ec2-app-deploy.md) | **EC2 #1 수동 배포 런북** — Docker 설치·EC2 IAM(ECR pull)·pull/up 검증 (8-4) |
| 07 | [07-aws-operations-log.md](07-aws-operations-log.md) | **AWS 작업 로그 & 관리 대장** — 실행 명령·리소스 상태·되돌리는 법·관리 체크리스트 |

## 결정 로그 (요약)

- **DB 배치**: EC2 한 대에 MySQL 컨테이너 동거 (docker-compose) — 진짜 무료, 관리 포인트 1개
- **빌드/배포**: 수동 (로컬 빌드 → 아티팩트 전송). *마이크로 인스턴스에서 Gradle 빌드는 OOM 위험이라 서버 빌드 금지*
- **HTTPS**: 현 단계는 HTTP만 (8080)
- **리전**: ap-northeast-2 (서울) 기준

## 진행 상태

- [x] 로컬 리소스 실측 (JVM + MySQL)
- [x] 문제점 도출
- [x] 인스턴스 후보/비용 비교
- [x] 빌드 가능화 (컴파일 복구 → [02](02-build-fix.md))
- [x] prod 프로파일 분리 (`application-prod.yml`: validate·시크릿 외부화·SQL로그 off)
- [x] actuator 헬스체크 + `SecurityConfig` `/actuator/health` permitAll
- [x] Dockerfile + `docker-compose.prod.yml` (buffer pool 128M·healthcheck·restart)
- [x] **Phase 1 게이트: 로컬 prod 컨테이너 기동 검증** — `{"status":"UP"}`, 스키마 6테이블 생성 (2026-07-02)
- [ ] ~~EC2 셋업 런북~~ → **cloud(AWS) 배포는 개발 완성 후로 보류**. `application-prod.yml`·`docker-compose.prod.yml` 제거됨. 재개 시 GitHub Actions CI/CD(develop 머지→ECR→EC2) 방향.
