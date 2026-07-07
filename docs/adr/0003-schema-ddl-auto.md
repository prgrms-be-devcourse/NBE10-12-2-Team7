# 0003. 스키마를 Hibernate ddl-auto로 관리 (마이그레이션 도구 미도입)

> 상태: Accepted (caveat) · 날짜: 2026-07-07

## 배경 (Context)

DB 스키마를 어떻게 관리할지 결정이 필요했다. 후보는 **Hibernate `ddl-auto`**(엔티티에서 자동 반영)와 **마이그레이션 도구**(Flyway/Liquibase, 버전드 SQL).

현재 상태(실측): `application-dev.yml`·`application-local.yml` 모두 `ddl-auto: update`, Flyway/Liquibase 미사용. **클라우드 앱 EC2도 `SPRING_PROFILES_ACTIVE=local`** 로 기동해 같은 설정을 재사용한다(전용 prod 프로파일 없음).

## 결정 (Decision)

현 단계에서는 **`ddl-auto: update`로 스키마를 자동 관리**하고, 별도 마이그레이션 도구를 도입하지 않는다. 빠른 개발 반복을 우선한다.

## 결과 (Consequences)

**좋은 점**
- 엔티티만 바꾸면 스키마가 따라와 개발 속도가 빠르다. 초기 스키마 변동이 잦은 단계에 적합.

**감수하는 비용 / ⚠️ 위험**
- **운영 DB에도 `ddl-auto: update`가 적용된다** → 배포 시 Hibernate가 운영 스키마를 자동 변경할 수 있다. 컬럼 삭제·타입 변경 등은 `update`가 처리하지 못하거나 데이터 사고로 이어질 수 있다.
- 스키마 변경 이력이 코드(엔티티)에만 있고 **버전드 기록이 없다** → 롤백·감사 어려움.

**후속 (권장)**
- 운영 안정화 시점에 **Flyway/Liquibase 도입 + 운영 프로파일은 `ddl-auto: validate`(또는 none)** 로 전환하는 새 ADR 작성.
- 그 전까지는 운영 배포 전 스키마 변경 영향을 수동 점검한다.

## 대안 (Alternatives)

- **Flyway/Liquibase 즉시 도입**: 운영 안전·이력 관리에 정석이나, 초기 스키마 변동이 잦아 마이그레이션 파일 관리 오버헤드가 큼 → 현 단계에선 보류(후속 전환 전제).
- **운영만 `validate`, 개발만 `update`**: 전용 prod 프로파일이 필요. 프로파일 분리와 함께 위 후속에서 처리.
