# DB 마이그레이션 (Flyway)

> 최종 수정일: 2026-07-08 · 상태: draft

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
