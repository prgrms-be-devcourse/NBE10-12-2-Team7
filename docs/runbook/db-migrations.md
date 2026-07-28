# DB 마이그레이션 (Flyway)

> 최종 수정일: 2026-07-28 · 상태: draft

스키마는 프로파일에 따라 관리 방식이 다르다([ADR 0005](../adr/0005-flyway-migration.md)):

| 프로파일 | 스키마 관리 | 마이그레이션 파일 적용 |
| --- | --- | --- |
| **prod** | Flyway(`ddl-auto: validate`) | 앱 기동 시 `backend/src/main/resources/db/migration/V*.sql`을 자동 적용 |
| **dev** | Hibernate `ddl-auto: update` | Flyway 꺼짐. **컬럼/테이블 삭제는 update가 처리 못 함** — 아래 "dev에서" 참고 |
| **test** | Hibernate `ddl-auto: create-drop`(H2) | 테스트마다 엔티티 기준으로 새로 생성 — 항상 최신 상태, 조치 불필요 |

## 새 마이그레이션이 필요할 때

엔티티 변경으로 컬럼/테이블을 **추가**하는 것은 prod에서도 자동 처리되지 않는다 — Flyway가 스키마를 전담하므로, 엔티티만 바꾸고 마이그레이션 파일을 빼먹으면 `ddl-auto: validate`가 기동 시점에 검증 실패로 앱을 멈춘다. 엔티티를 바꿀 때마다 `backend/src/main/resources/db/migration/`에 `V{다음 번호}__{설명}.sql`을 함께 추가한다(번호는 기존 파일 중 최댓값 + 1, 되돌리거나 재사용하지 않는다).

## prod에서

배포 시 Flyway가 자동으로 적용한다. 별도 수동 작업이 없다 — CI/CD가 새 이미지를 올리고 앱이 기동되는 순간 미적용 `V*.sql`이 순서대로 실행된다.

## dev에서

Flyway가 꺼져 있어 컬럼/테이블을 **줄이는** 마이그레이션(V2처럼 DROP이 포함된 파일)은 로컬 DB에 자동 반영되지 않는다. 둘 중 하나를 선택한다.

1. **DB를 새로 만든다(권장 — dev 데이터는 어차피 disposable)**:
   ```bash
   docker compose down -v   # dongne-mysql-data 볼륨까지 삭제
   docker compose up -d --wait
   ```
   이후 `ddl-auto: update`가 엔티티 기준으로 처음부터 스키마를 만들어, 옛 컬럼/테이블이 아예 생기지 않는다.
2. **기존 로컬 데이터를 유지해야 한다면**, 해당 마이그레이션 파일의 SQL을 직접 실행한다:
   ```bash
   docker exec -i dongne-mysql mysql -udongne -pdongne1234 dongne_market < backend/src/main/resources/db/migration/V2__drop_unused_email_verification_and_password_reset_columns.sql
   ```

## 마이그레이션 이력

| 버전 | 파일 | 내용 |
| --- | --- | --- |
| V1 | [`V1__baseline.sql`](../../backend/src/main/resources/db/migration/V1__baseline.sql) | Flyway 도입 시점 기존 스키마 전체(16테이블+FK) 베이스라인 |
| V2 | [`V2__drop_unused_email_verification_and_password_reset_columns.sql`](../../backend/src/main/resources/db/migration/V2__drop_unused_email_verification_and_password_reset_columns.sql) | 인증 TTL 데이터를 Redis로 이전([ADR 0006](../adr/0006-redis-for-auth-ttl-data.md))하며 미사용이 된 `email_verifications.code`/`sent_at`/`expires_at` 컬럼 제거, `password_reset_tokens` 테이블 제거 |
| V3 | [`V3__region_hierarchy_and_product_region_fk.sql`](../../backend/src/main/resources/db/migration/V3__region_hierarchy_and_product_region_fk.sql) | 계층형 `regions` 구조와 `products.region_id` FK 추가. 기존 문자열 컬럼은 호환을 위해 유지 |
| V4 | [`V4__member_locations_region_fk.sql`](../../backend/src/main/resources/db/migration/V4__member_locations_region_fk.sql) | `member_locations.region_id` FK 추가. 기존 문자열 컬럼은 호환을 위해 유지 |
| V5 | [`V5__drop_region_string_compatibility.sql`](../../backend/src/main/resources/db/migration/V5__drop_region_string_compatibility.sql) | 백필 완료 후 문자열 호환 컬럼 제거: `products.region`, `member_locations.region`, `regions.name` 제거. `products.region_id`, `member_locations.region_id`를 `NOT NULL`로 강화 |

## 지역 문자열 호환 제거 검증 기록

2026-07-28에 빈 로컬 MySQL 스키마 `dongne_market_flyway_v5_check`를 별도로 만들어 V1~V5를 순서대로 실행했다. 운영 DB와 기존 로컬 `dongne_market` 스키마는 건드리지 않았다.

검증 결과:

- Flyway V1~V5 적용 성공
- `products.region` 컬럼 없음
- `member_locations.region` 컬럼 없음
- `regions.name` 컬럼 없음
- `products.region_id` 존재, `NOT NULL`
- `member_locations.region_id` 존재, `NOT NULL`

검증용 스키마는 확인 후 삭제했다.

## 현재 Flyway 누락 테이블

V1~V5 적용 후 `ddl-auto=validate`로 앱을 기동하면 아래 엔티티 테이블이 Flyway 마이그레이션에 없어 검증에 실패한다. 지역 문자열 제거와 직접 관련된 실패는 아니며, 별도 DB 마이그레이션 정리 대상이다.

| 누락 테이블 | 소유 도메인 | 근거 |
| --- | --- | --- |
| `escrows` | escrow | `Escrow` 엔티티는 있으나 V1~V5에 `create table escrows` 없음 |
| `manner_scores` | manner | `MannerScore` 엔티티는 있으나 V1~V5에 `create table manner_scores` 없음 |
| `manner_score_histories` | manner | `MannerScoreHistory` 엔티티는 있으나 V1~V5에 `create table manner_score_histories` 없음 |
| `manner_ratings` | manner | `MannerRating` 엔티티는 있으나 V1~V5에 `create table manner_ratings` 없음 |
