# 0005. Flyway 도입 + 운영(prod) 스키마를 ddl-auto: validate로

> 상태: Accepted · 날짜: 2026-07-08 · [ADR 0003](0003-schema-ddl-auto.md)을 대체(supersede)

## 배경 (Context)

[ADR 0003](0003-schema-ddl-auto.md)은 초기 개발 속도를 위해 **`ddl-auto: update`로 스키마를 자동 관리**하고 마이그레이션 도구를 도입하지 않기로 했다. 대신 후속으로 "운영 안정화 시점에 Flyway 도입 + 운영 프로파일 `validate` 전환"을 예고했다.

이제 그 조건이 갖춰졌다:
- [ADR 0004](0004-infra-boundary.md) P2에서 **`prod` 프로파일이 분리**돼, 운영에만 다른 스키마 정책을 적용할 자리가 생겼다.
- `update`의 위험(운영 스키마 자동변경, 컬럼 삭제·타입변경 불가, 변경 이력 부재)은 팀이 커지고 배포가 잦아질수록 커진다.

## 결정 (Decision)

**Flyway를 도입**해 스키마를 버전드 SQL(`backend/src/main/resources/db/migration/V*.sql`)로 관리한다. 프로파일별 역할:

| 프로파일 | ddl-auto | Flyway | 스키마 담당 |
| --- | --- | --- | --- |
| `dev` | `update` | OFF | Hibernate (빠른 개발 루프 유지) |
| `test` | `create-drop` (H2) | OFF | Hibernate (H2라 MySQL baseline 부적합) |
| **`prod`** | **`validate`** | **ON** | **Flyway** (Hibernate는 검증만) |

- **베이스라인**: 현재 스키마 전체를 [`V1__baseline.sql`](../../backend/src/main/resources/db/migration/V1__baseline.sql)로 고정(Hibernate 스키마 스크립트 생성 = 엔티티 메타데이터 → MySQL DDL).
- **`baseline-on-migrate: true`, `baseline-version: 1`**: 빈 DB(신규 온프레미스)는 V1부터 실행해 스키마 생성, 기존 스키마가 있는 DB(운영 클라우드)는 V1을 baseline으로 간주하고 V2부터 적용.
- 이후 스키마 변경은 엔티티 수정과 **짝지어 `V2`, `V3`… SQL을 추가**한다.

## 결과 (Consequences)

**좋은 점**
- 운영 스키마 변경이 코드 리뷰 가능한 SQL로 남고, 이력(`flyway_schema_history`)·롤백 기준이 생긴다.
- `validate`가 **엔티티와 실제 스키마 불일치를 기동 시점에 차단**한다 → "엔티티만 바꾸고 마이그레이션을 빠뜨린" 배포가 조용히 나가지 않는다.
- 적용된 V파일은 checksum으로 고정돼 사후 변조를 막는다.

**감수하는 비용 / ⚠️ 위험**
- **dev-prod 스키마 드리프트 가능**: dev는 `update`라 엔티티만 바꿔도 반영되지만 prod는 V파일이 있어야 한다 → "엔티티 바꾸면 V파일도 같이" 규율 필요(후속: CI에서 MySQL+Flyway+validate 부팅 검사).
- **클라우드 기존 DB 대조 필수**: 운영 DB는 그동안 `update` 누적본이라 V1 baseline과 미세 차이(인덱스명·컬럼순서 등)가 있을 수 있다. `validate` 켜기 전 실제 운영 스키마를 덤프해 V1과 대조/보정해야 한다.
- V1은 실행 후 수정 불가 — 보정이 필요하면 V2로 처리.

**검증 (완료)**
- 빈 DB에 `prod` 부팅 → Flyway가 V1 적용(스키마 생성) → Hibernate `validate` 통과 → 정상 기동 확인.
- 기존 테스트 스위트(H2, Flyway OFF)는 그대로 통과.

## 대안 (Alternatives)

- **Liquibase**: 기능은 동등하나 XML/YAML 자체 DSL 학습·관리 오버헤드가 있다. SQL에 익숙한 소규모 팀엔 순수 SQL인 Flyway가 단순 → Flyway 채택.
- **현행 유지(`update`)**: 세팅 비용 0이나 운영 스키마 위험 존치 → 기각.
- **dev도 Flyway+validate로 통일**: 드리프트를 원천 차단하나, 활발한 기능 개발 중 매 엔티티 변경마다 V파일을 강제해 개발 속도를 떨어뜨린다 → 지금은 prod 전용, 드리프트는 규율/CI로 관리.
