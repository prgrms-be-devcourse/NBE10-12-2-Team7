# 기술 결정 기록 (ADR)

[Architecture Decision Record](https://adr.github.io/) — **"왜 이렇게 정했나"** 를 남기는 곳. 코드는 *무엇을* 보여주지만 ADR은 *왜/대안/트레이드오프*를 남겨, 나중에 같은 논쟁을 반복하지 않게 한다.

> 최종 수정일: 2026-07-07 · 상태: draft

## 작성 규칙

- 하나의 결정 = 하나의 파일. 번호순 `NNNN-제목.md` (`0001-`, `0002-` …).
- **결정은 지우거나 고치지 않는다.** 뒤집을 땐 새 ADR을 쓰고, 옛 ADR 상태를 `Superseded by 000X`로 바꾼다.
- 짧게. 배경·결정·결과·대안이면 충분하다. 템플릿은 아래.
- 상태값: `Proposed` → `Accepted` → (`Superseded` / `Deprecated`).

## 목록

| 번호 | 제목 | 상태 |
| --- | --- | --- |
| [0001](0001-adopt-docs-as-code-structure.md) | docs를 Docs-as-Code 구조로 재편 | Accepted |
| [0002](0002-db-hosting-ec2-mysql.md) | 운영 DB를 EC2 자체 호스팅 MySQL로 (RDS 미사용) | Accepted |
| [0003](0003-schema-ddl-auto.md) | 스키마를 ddl-auto로 관리 (마이그레이션 도구 미도입) | Superseded by 0005 |
| [0004](0004-infra-boundary.md) | 인프라를 2축(운영 수위 × 배포 지형)으로 경계 재정리 | Accepted |
| [0005](0005-flyway-migration.md) | Flyway 도입 + 운영(prod) 스키마를 ddl-auto: validate로 | Accepted |
| [0006](0006-redis-for-auth-ttl-data.md) | 인증 관련 TTL 데이터(Refresh Token/로그인 실패 제한/이메일 인증 코드/비밀번호 재설정 토큰)를 Redis로 | Accepted |

---

## 템플릿

```markdown
# NNNN. <결정 제목>

> 상태: Proposed | Accepted | Superseded by 000X · 날짜: YYYY-MM-DD

## 배경 (Context)
어떤 문제/상황이라서 결정이 필요했나.

## 결정 (Decision)
무엇을 하기로 했나. (한 문장으로 단언)

## 결과 (Consequences)
좋은 점 / 감수하는 비용 / 후속 작업.

## 대안 (Alternatives)
고려했지만 택하지 않은 것과 그 이유.
```
